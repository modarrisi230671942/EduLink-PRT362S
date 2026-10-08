import { Container } from 'react-bootstrap';
import { Brand } from './AppNavbar.jsx';

export default function Footer() {
  return (
    <footer className="app-footer">
      <Container className="d-flex flex-column flex-md-row justify-content-between align-items-center gap-2">
        <Brand />
        <span className="small text-muted">
          Connecting students with verified employers · PRT372S Final Year Project · {new Date().getFullYear()}
        </span>
      </Container>
    </footer>
  );
}
