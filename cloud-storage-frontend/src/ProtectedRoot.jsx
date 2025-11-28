import { useAuth } from "./AuthContext";
import { Navigate } from "react-router-dom";

export default function ProtectedRoute({ children }) {
  const { token, loading } = useAuth();

  if (loading) return <div>Загрузка...</div>;
  if (!token) return <Navigate to="/login" />;

  return children;
}
