// src/App.jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './AuthContext';
import Login from './components/Login';
import Register from './components/Register';
import FileBrowser from './components/FileBrowser';
import Landing from './pages/Landing';

function PrivateRoute({ children }) {
  const { isAuthenticated, loading } = useAuth();

  console.log("PrivateRoute: checking access", {
    isAuthenticated,
    loading,
    hasToken: !!localStorage.getItem("token")
  });

  if (loading) {
    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 flex items-center justify-center">
          <div className="text-white text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-white mx-auto mb-4"></div>
            <p className="text-xl">Загрузка...</p>
          </div>
        </div>
    );
  }

  if (!isAuthenticated) {
    console.log("PrivateRoute: not authenticated, redirecting to /login");
    return <Navigate to="/login" />;
  }

  console.log("PrivateRoute: authenticated, rendering children");
  return children;
}

function App() {
  return (
      <AuthProvider>
        <Router>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route
                path="/files/*"
                element={
                  <PrivateRoute>
                    <FileBrowser />
                  </PrivateRoute>
                }
            />
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </Router>
      </AuthProvider>
  );
}

export default App;