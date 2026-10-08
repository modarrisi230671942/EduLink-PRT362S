import { useState } from 'react';
import { Button, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/client.js';
import { HOME_BY_ROLE, useAuth } from '../context/AuthContext.jsx';
import { useToast } from '../context/ToastContext.jsx';
import { passwordProblem } from '../utils/format.js';

const STUDENT_FIELDS = { email: '', password: '', fullName: '', studentNumber: '', course: '', institution: '', graduationYear: new Date().getFullYear() + 1, skills: '' };
const COMPANY_FIELDS = { email: '', password: '', companyName: '', industry: '', location: '', website: '' };

function Field({ name, label, form, errors, setForm, as, type = 'text', hint, ...props }) {
  return (
    <Form.Group controlId={name}>
      <Form.Label>{label}</Form.Label>
      <Form.Control
        as={as}
        type={type}
        value={form[name]}
        isInvalid={Boolean(errors[name])}
        onChange={(e) => setForm({ ...form, [name]: e.target.value })}
        {...props}
      />
      <Form.Control.Feedback type="invalid">{errors[name]}</Form.Control.Feedback>
      {hint && !errors[name] && <Form.Text>{hint}</Form.Text>}
    </Form.Group>
  );
}

export default function RegisterPage() {
  const { user, registerStudent, registerCompany } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [role, setRole] = useState('STUDENT');
  const [student, setStudent] = useState(STUDENT_FIELDS);
  const [company, setCompany] = useState(COMPANY_FIELDS);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  if (user) return <Navigate to={HOME_BY_ROLE[user.role]} replace />;

  const form = role === 'STUDENT' ? student : company;
  const setForm = role === 'STUDENT' ? setStudent : setCompany;
  const common = { form, errors, setForm };

  const submit = async (event) => {
    event.preventDefault();
    const weak = passwordProblem(form.password);
    if (weak) {
      setErrors({ password: weak });
      return;
    }
    setSubmitting(true);
    setErrors({});
    setFormError(null);
    try {
      if (role === 'STUDENT') {
        await registerStudent({ ...student, graduationYear: Number(student.graduationYear) });
        toast.success('Welcome to EduLink! Add your CV to stand out to employers.');
        navigate('/student/profile');
      } else {
        await registerCompany(company);
        toast.success('Account created. An administrator will verify your company shortly.');
        navigate(HOME_BY_ROLE.COMPANY);
      }
    } catch (err) {
      setErrors(fieldErrors(err));
      setFormError(errorMessage(err, 'Registration failed.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Container className="auth-page">
      <div className="auth-card auth-card-wide">
        <h1 className="h4 fw-bold text-center mb-1">Create your EduLink account</h1>
        <p className="text-muted small text-center mb-4">It takes less than two minutes.</p>

        <div className="role-switch mb-4" role="tablist">
          {[['STUDENT', 'mortarboard', "I'm a student"], ['COMPANY', 'building', "I'm an employer"]].map(([value, icon, label]) => (
            <button key={value} type="button" role="tab" aria-selected={role === value}
                    className={role === value ? 'active' : ''}
                    onClick={() => { setRole(value); setErrors({}); setFormError(null); }}>
              <i className={`bi bi-${icon} me-2`} aria-hidden="true" />{label}
            </button>
          ))}
        </div>

        {formError && <div className="alert alert-danger py-2 small">{formError}</div>}

        <Form onSubmit={submit} noValidate>
          <Row className="g-3">
            <Col md={6}><Field name="email" label="Email" type="email" autoComplete="email" required {...common} /></Col>
            <Col md={6}><Field name="password" label="Password" type="password" autoComplete="new-password" required hint="8+ characters with a letter and a number" {...common} /></Col>

            {role === 'STUDENT' ? (
              <>
                <Col md={6}><Field name="fullName" label="Full name" required {...common} /></Col>
                <Col md={6}><Field name="studentNumber" label="Student number" required {...common} /></Col>
                <Col md={6}><Field name="course" label="Course / qualification" required {...common} /></Col>
                <Col md={6}><Field name="institution" label="Institution" placeholder="e.g. CPUT" required {...common} /></Col>
                <Col md={4}><Field name="graduationYear" label="Graduation year" type="number" min={2000} max={2100} required {...common} /></Col>
                <Col md={8}><Field name="skills" label="Skills" placeholder="Java, SQL, Python" hint="Comma-separated — used to match you with jobs" {...common} /></Col>
              </>
            ) : (
              <>
                <Col md={6}><Field name="companyName" label="Company name" required {...common} /></Col>
                <Col md={6}><Field name="industry" label="Industry" placeholder="e.g. Software Development" {...common} /></Col>
                <Col md={6}><Field name="location" label="Location" placeholder="e.g. Cape Town" {...common} /></Col>
                <Col md={6}><Field name="website" label="Website" type="url" placeholder="https://" {...common} /></Col>
                <Col xs={12}>
                  <div className="alert alert-info small mb-0">
                    <i className="bi bi-info-circle me-2" aria-hidden="true" />
                    To protect students, new employers are verified by an administrator before they can post jobs.
                  </div>
                </Col>
              </>
            )}
          </Row>

          <Button type="submit" size="lg" className="w-100 mt-4" disabled={submitting}>
            {submitting && <Spinner size="sm" className="me-2" />}Create account
          </Button>
        </Form>

        <p className="text-center small text-muted mt-4 mb-0">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </div>
    </Container>
  );
}
