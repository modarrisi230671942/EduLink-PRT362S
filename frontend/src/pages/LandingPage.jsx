import { useEffect, useState } from 'react';
import { Col, Container, Row } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { jobsApi, publicApi } from '../api/endpoints.js';
import { HOME_BY_ROLE, useAuth } from '../context/AuthContext.jsx';
import JobCard from '../components/JobCard.jsx';
import JobDetailModal from '../components/JobDetailModal.jsx';

const FEATURES = [
  { icon: 'stars', title: 'Skill-match scoring', text: 'Every job shows how well your skills fit — and exactly which skills to learn next.' },
  { icon: 'patch-check', title: 'Verified employers only', text: 'Companies are vetted by an administrator before they can post, keeping students safe from fake listings.' },
  { icon: 'file-earmark-person', title: 'One-click CV', text: 'Upload your PDF CV once; employers can view it with every application you send.' },
  { icon: 'bell', title: 'Real-time updates', text: 'Get notified the moment an employer reviews, accepts or declines your application.' },
];

const STEPS = [
  { role: 'Students', icon: 'mortarboard', points: ['Build a profile with your skills and CV', 'Get ranked job recommendations', 'Apply and track every application'] },
  { role: 'Employers', icon: 'building', points: ['Register and get verified', 'Post graduate roles and internships', 'Review applicants ranked by skill match'] },
  { role: 'Administrators', icon: 'shield-check', points: ['Verify employers before they post', 'Manage user accounts', 'Monitor platform analytics'] },
];

export default function LandingPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    publicApi.stats().then(setStats).catch(() => {});
    jobsApi.search({ size: 3 }).then((page) => setJobs(page.content)).catch(() => {});
  }, []);

  return (
    <>
      <section className="hero">
        <Container>
          <Row className="align-items-center g-5">
            <Col lg={7}>
              <span className="eyebrow"><i className="bi bi-mortarboard" aria-hidden="true" /> Built for South African students &amp; graduates</span>
              <h1 className="hero-title">
                Your first job shouldn&apos;t be <span className="text-gradient">a guessing game.</span>
              </h1>
              <p className="hero-lead">
                EduLink connects students and recent graduates with verified employers. See how well you match each
                role, apply with your CV in one click, and follow every application in real time.
              </p>
              <div className="d-flex flex-wrap gap-2">
                {user ? (
                  <Link to={HOME_BY_ROLE[user.role]} className="btn btn-primary btn-lg">Go to my dashboard</Link>
                ) : (
                  <>
                    <Link to="/register" className="btn btn-primary btn-lg">Create a free account</Link>
                    <Link to="/jobs" className="btn btn-outline-primary btn-lg">Browse jobs</Link>
                  </>
                )}
              </div>
            </Col>
            <Col lg={5}>
              <div className="hero-stats">
                <div className="hero-stat"><strong>{stats?.openJobs ?? '–'}</strong><span>open opportunities</span></div>
                <div className="hero-stat"><strong>{stats?.verifiedCompanies ?? '–'}</strong><span>verified employers</span></div>
                <div className="hero-stat"><strong>{stats?.students ?? '–'}</strong><span>registered students</span></div>
              </div>
            </Col>
          </Row>
        </Container>
      </section>

      <section className="section">
        <Container>
          <div className="section-heading">
            <h2>The problem we solve</h2>
            <p>
              Students struggle to find entry-level roles that fit their skills, send applications into the void and
              never hear back, and are exposed to fake job adverts. Employers wade through unsuitable applicants.
              EduLink fixes all four.
            </p>
          </div>
          <Row className="g-4">
            {FEATURES.map((f) => (
              <Col md={6} lg={3} key={f.title}>
                <div className="feature-card">
                  <i className={`bi bi-${f.icon}`} aria-hidden="true" />
                  <h3>{f.title}</h3>
                  <p>{f.text}</p>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>

      {jobs.length > 0 && (
        <section className="section section-alt">
          <Container>
            <div className="d-flex justify-content-between align-items-end mb-4">
              <div className="section-heading mb-0 mx-0 text-start">
                <h2>Latest opportunities</h2>
              </div>
              <Link to="/jobs" className="btn btn-link">View all jobs <i className="bi bi-arrow-right" aria-hidden="true" /></Link>
            </div>
            <Row className="g-4">
              {jobs.map((job) => (
                <Col md={6} lg={4} key={job.jobId}><JobCard job={job} onOpen={setSelected} /></Col>
              ))}
            </Row>
          </Container>
        </section>
      )}

      <section className="section">
        <Container>
          <div className="section-heading"><h2>How it works</h2></div>
          <Row className="g-4">
            {STEPS.map((s) => (
              <Col md={4} key={s.role}>
                <div className="step-card">
                  <div className="step-icon"><i className={`bi bi-${s.icon}`} aria-hidden="true" /></div>
                  <h3>For {s.role.toLowerCase()}</h3>
                  <ul>{s.points.map((p) => <li key={p}>{p}</li>)}</ul>
                </div>
              </Col>
            ))}
          </Row>
        </Container>
      </section>

      <JobDetailModal job={selected} onHide={() => setSelected(null)} />
    </>
  );
}
