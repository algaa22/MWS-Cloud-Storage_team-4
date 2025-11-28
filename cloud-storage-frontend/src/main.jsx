import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App.jsx";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./AuthContext";   // ← добавить
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")).render(
    <BrowserRouter>
      <AuthProvider>       {/* ← оборачиваем всё приложение */}
        <App />
      </AuthProvider>
    </BrowserRouter>
);
