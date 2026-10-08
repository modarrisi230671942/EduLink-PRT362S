import { Container } from 'react-bootstrap';
import { Link } from 'react-router-dom';
import { EmptyState } from '../components/ui.jsx';

export default function NotFoundPage() {
  return (
    <Container className="page">
      <EmptyState icon="signpost-split" title="Page not found"
                  action={<Link to="/" className="btn btn-primary">Back to home</Link>}>
        The page you are looking for does not exist or has moved.
      </EmptyState>
    </Container>
  );
}
