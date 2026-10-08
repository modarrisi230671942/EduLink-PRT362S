package za.ac.mycput.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.ac.mycput.domain.Company;
import za.ac.mycput.domain.Student;
import za.ac.mycput.domain.User;
import za.ac.mycput.domain.enums.ApplicationStatus;
import za.ac.mycput.domain.enums.AuditAction;
import za.ac.mycput.security.AuthUser;
import za.ac.mycput.service.IAuditService;
import za.ac.mycput.domain.enums.JobType;
import za.ac.mycput.domain.enums.NotificationType;
import za.ac.mycput.domain.enums.UserType;
import za.ac.mycput.dto.AdminDtos.*;
import za.ac.mycput.dto.CommonDtos.PageResponse;
import za.ac.mycput.dto.ProfileDtos.CompanyResponse;
import za.ac.mycput.exception.ApiException;
import za.ac.mycput.repository.*;
import za.ac.mycput.service.IAdminService;
import za.ac.mycput.service.INotificationService;
import za.ac.mycput.service.support.DtoMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements IAdminService {

    private static final int MONTHS_IN_TREND = 6;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final INotificationService notifications;
    private final IAuditService audit;

    public AdminServiceImpl(UserRepository userRepository,
                            StudentRepository studentRepository,
                            CompanyRepository companyRepository,
                            JobPostingRepository jobPostingRepository,
                            ApplicationRepository applicationRepository,
                            INotificationService notifications,
                            IAuditService audit) {
        this.audit = audit;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.notifications = notifications;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        long accepted = applicationRepository.countByStatus(ApplicationStatus.ACCEPTED);
        long rejected = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        long decided = accepted + rejected;

        Totals totals = new Totals(
                studentRepository.count(),
                companyRepository.count(),
                companyRepository.countByIsVerified(true),
                companyRepository.countByIsVerified(false),
                jobPostingRepository.count(),
                jobPostingRepository.countOpenJobs(LocalDate.now()),
                applicationRepository.count(),
                accepted,
                decided,
                decided == 0 ? 0 : Math.round(accepted * 100f / decided));

        return new AdminStatsResponse(
                totals,
                applicationsPerMonth(),
                countsForEveryValue(ApplicationStatus.values(), applicationRepository.countByStatusGrouped(),
                        status -> capitalise(status.name())),
                countsForEveryValue(JobType.values(), jobPostingRepository.countByJobType(),
                        type -> capitalise(type.dbValue())),
                applicationRepository.countPerCompany(PageRequest.of(0, 5)).stream()
                        .map(row -> new LabelCount((String) row[0], (Long) row[1]))
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicStatsResponse publicStats() {
        return new PublicStatsResponse(
                jobPostingRepository.countOpenJobs(LocalDate.now()),
                companyRepository.countByIsVerified(true),
                studentRepository.count());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> users(UserType role, String search, int page, int size) {
        String pattern = (search == null || search.isBlank()) ? null : "%" + search.trim().toLowerCase() + "%";
        var users = userRepository.search(role, pattern,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "createdAt")));

        // Look up display names for the whole page in two queries instead of one per user
        List<Integer> ids = users.getContent().stream().map(User::getUserId).toList();
        Map<Integer, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Student s : studentRepository.findByUser_UserIdIn(ids)) {
                names.put(s.getUser().getUserId(), s.getFullName());
            }
            for (Company c : companyRepository.findByUser_UserIdIn(ids)) {
                names.put(c.getUser().getUserId(), c.getCompanyName());
            }
        }
        return PageResponse.of(users, u -> DtoMapper.toAdminUserResponse(u,
                u.getUserType() == UserType.ADMIN ? "System Administrator" : names.getOrDefault(u.getUserId(), "—")));
    }

    @Override
    @Transactional
    public AdminUserResponse setUserActive(AuthUser admin, Integer userId, boolean active) {
        if (admin.userId().equals(userId)) {
            throw ApiException.badRequest("You cannot disable your own account.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        boolean wasActive = user.isActive();
        user.setActive(active);
        if (active && !wasActive) {
            notifications.notify(user, NotificationType.ACCOUNT_ENABLED,
                    "Your account has been re-activated by the administrator.", null);
        }
        if (active != wasActive) {
            audit.record(admin.userId(), admin.email(), active ? AuditAction.USER_ENABLED : AuditAction.USER_DISABLED,
                    "USER", userId, user.getEmail());
        }
        String displayName = switch (user.getUserType()) {
            case STUDENT -> studentRepository.findByUser_UserId(userId).map(Student::getFullName).orElse("—");
            case COMPANY -> companyRepository.findByUser_UserId(userId).map(Company::getCompanyName).orElse("—");
            case ADMIN -> "System Administrator";
        };
        return DtoMapper.toAdminUserResponse(user, displayName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyResponse> companies(Boolean verified) {
        // Unverified companies first, so the admin sees what needs attention
        Sort sort = Sort.by(Sort.Order.asc("isVerified"), Sort.Order.asc("companyName"));
        List<Company> companies = verified == null
                ? companyRepository.findAllBy(sort)
                : companyRepository.findByIsVerified(verified, sort);
        return companies.stream().map(DtoMapper::toCompanyResponse).toList();
    }

    @Override
    @Transactional
    public CompanyResponse setCompanyVerified(AuthUser admin, Integer companyId, boolean verified) {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> ApiException.notFound("Company"));
        if (company.isVerified() != verified) {
            company.setVerified(verified);
            audit.record(admin.userId(), admin.email(),
                    verified ? AuditAction.COMPANY_VERIFIED : AuditAction.COMPANY_VERIFICATION_REVOKED,
                    "COMPANY", companyId, company.getCompanyName());
            notifications.notify(company.getUser(),
                    verified ? NotificationType.COMPANY_VERIFIED : NotificationType.COMPANY_VERIFICATION_REVOKED,
                    verified ? "Your company has been verified. You can now post job vacancies."
                             : "Your company's verification has been revoked. Contact the administrator for details.",
                    "/company/jobs");
        }
        return DtoMapper.toCompanyResponse(company);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Applications per calendar month for the last six months (including months with zero). */
    private List<LabelCount> applicationsPerMonth() {
        YearMonth current = YearMonth.now();
        YearMonth first = current.minusMonths(MONTHS_IN_TREND - 1L);
        LocalDateTime since = first.atDay(1).atStartOfDay();

        Map<YearMonth, Long> counts = applicationRepository.findAppliedDatesSince(since).stream()
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));

        List<LabelCount> result = new ArrayList<>();
        for (YearMonth month = first; !month.isAfter(current); month = month.plusMonths(1)) {
            result.add(new LabelCount(month.format(MONTH_LABEL), counts.getOrDefault(month, 0L)));
        }
        return result;
    }

    /** Turns [enum, count] rows into one LabelCount per enum value, filling in zeros. */
    private static <E extends Enum<E>> List<LabelCount> countsForEveryValue(E[] values, List<Object[]> rows,
                                                                           Function<E, String> label) {
        Map<Object, Long> byValue = new HashMap<>();
        for (Object[] row : rows) {
            byValue.put(row[0], (Long) row[1]);
        }
        return Arrays.stream(values)
                .map(value -> new LabelCount(label.apply(value), byValue.getOrDefault(value, 0L)))
                .toList();
    }

    private static String capitalise(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace('_', '-');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
