import { render, screen } from '@testing-library/react';
import LandingPage from './pages/LandingPage';

jest.mock('react-router-dom', () => ({
  Link: ({ to, children }) => <a href={to}>{children}</a>
}), { virtual: true });

test('renders the booking landing page', () => {
  render(<LandingPage />);
  expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Сайт записи в профилакторий');
});
