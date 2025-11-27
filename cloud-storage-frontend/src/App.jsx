import { Routes, Route } from "react-router-dom";
import Landing from "./Landing.jsx";
import Login from "./components/Login.jsx";
import Register from "./components/Register.jsx";
import FileBrowser from "./components/FileBrowser.jsx";

export default function App() {
  return (
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        navigate("/files");
        <Route path="/register" element={<Register />} />
        <Route path="/files" element={<FileBrowser />} />
      </Routes>
  );
}
