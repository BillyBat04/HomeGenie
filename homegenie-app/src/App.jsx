// App.jsx
import React, { useEffect, useState } from 'react';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import AssignmentModal from './components/AssignmentModal.jsx';
import AuthForm from './components/AuthForm.jsx';
import CreateRequestForm from './components/CreateRequestForm.jsx';
import Dashboard from './components/Dashboard.jsx';
import Header from './components/Header.jsx';
import { API_BASE_MAINTENANCE, API_BASE_USER } from './utils/constants.js';

const HomeGenieApp = () => {
  const [currentUser, setCurrentUser] = useState(null);
  const [view, setView] = useState('login');
  const [requests, setRequests] = useState([]);
  const [technicians, setTechnicians] = useState([]);
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showAssignModal, setShowAssignModal] = useState(false);

  const [authForm, setAuthForm] = useState({
    email: '',
    password: '',
    fullName: '',
    phoneNumber: '',
    flatNumber: ''
  });

  const [newRequest, setNewRequest] = useState({
    title: '',
    description: '',
    imageBase64: ''
  });

  useEffect(() => {
    const stored = localStorage.getItem('homegenieUser');
    if (stored) {
      const user = JSON.parse(stored);
      setCurrentUser(user);
      setView('dashboard');
      loadRequests(user);
    }
  }, []);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_USER}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: authForm.email,
          password: authForm.password
        })
      });

      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('homegenieUser', JSON.stringify(data));
        setCurrentUser(data);
        setView('dashboard');
        loadRequests(data);
      } else {
        toast.error('Login failed. Check credentials.');
      }
    } catch (err) {
      toast.error('Error: ' + err.message);
    }
    setLoading(false);
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_USER}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(authForm)
      });

      if (res.ok) {
        const data = await res.json();
        localStorage.setItem('homegenieUser', JSON.stringify(data));
        setCurrentUser(data);
        setView('dashboard');
        loadRequests(data);
      } else {
        toast.error('Registration failed');
      }
    } catch (err) {
      toast.error('Error: ' + err.message);
    }
    setLoading(false);
  };

  const loadRequests = async (user) => {
    setLoading(true);
    try {
      const endpoint = user.role === 'ADMIN'
        ? `${API_BASE_MAINTENANCE}/maintenance`
        : `${API_BASE_MAINTENANCE}/maintenance/user/${user.userId}`;

      const res = await fetch(endpoint);
      if (res.ok) {
        const data = await res.json();
        setRequests(data);
      }

      if (user.role === 'ADMIN') {
        const statsRes = await fetch(`${API_BASE_MAINTENANCE}/maintenance/statistics`);
        if (statsRes.ok) {
          setStats(await statsRes.json());
        }

        await loadTechnicians();
      }
    } catch (err) {
      console.error('Failed to load requests', err);
    }
    setLoading(false);
  };

  const loadTechnicians = async () => {
    try {
      const res = await fetch(`${API_BASE_MAINTENANCE}/maintenance/technicians`);
      if (res.ok) {
        const data = await res.json();
        setTechnicians(data);
      } else {
        console.error('Failed to fetch technicians');
      }
    } catch (err) {
      console.error('Failed to load technicians', err);
    }
  };

  const handleCreateRequest = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_MAINTENANCE}/maintenance`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': currentUser.userId.toString()
        },
        body: JSON.stringify(newRequest)
      });

      if (res.ok) {
        toast.success('✅ Request created! AI is analyzing and admin will be notified via email.');
        setNewRequest({ title: '', description: '', imageBase64: '' });
        setView('dashboard');
        loadRequests(currentUser);
      }
    } catch (err) {
      toast.error('Error: ' + err.message);
    }
    setLoading(false);
  };

  const updateRequestStatus = async (id, status) => {
    try {
      await fetch(`${API_BASE_MAINTENANCE}/maintenance/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status })
      });
      loadRequests(currentUser);
      toast.success('✅ Status updated! Resident will be notified via email.');
    } catch (err) {
      toast.error('Failed to update');
    }
  };

  const assignTechnician = async (requestId, technicianId) => {
    try {
      await fetch(`${API_BASE_MAINTENANCE}/maintenance/${requestId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          assignedTo: technicianId,
          status: 'IN_PROGRESS'
        })
      });
      setShowAssignModal(false);
      setSelectedRequest(null);
      loadRequests(currentUser);
      toast.success('✅ Technician assigned! They will receive an email notification.');
    } catch (err) {
      toast.error('Failed to assign technician');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('homegenieUser');
    setCurrentUser(null);
    setView('login');
    setRequests([]);
    setTechnicians([]);
  };

  const handleAssignRequest = (request) => {
    setSelectedRequest(request);
    setShowAssignModal(true);
  };

  if (!currentUser) {
    return (
      <AuthForm
        view={view}
        setView={setView}
        authForm={authForm}
        setAuthForm={setAuthForm}
        onLogin={handleLogin}
        onRegister={handleRegister}
        loading={loading}
      />
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header
        currentUser={currentUser}
        onLogout={handleLogout}
        setView={setView}
      />

      {view === 'dashboard' && (
        <Dashboard
          currentUser={currentUser}
          stats={stats}
          requests={requests}
          loading={loading}
          onAssign={handleAssignRequest}
          onUpdateStatus={updateRequestStatus}
          setView={setView}
          technicians={technicians}
        />
      )}

      {view === 'create' && (
        <CreateRequestForm
          newRequest={newRequest}
          setNewRequest={setNewRequest}
          onSubmit={handleCreateRequest}
          onCancel={() => setView('dashboard')}
          loading={loading}
        />
      )}

      {showAssignModal && (
        <AssignmentModal
          request={selectedRequest}
          technicians={technicians}
          onAssign={assignTechnician}
          onClose={() => {
            setShowAssignModal(false);
            setSelectedRequest(null);
          }}
        />
      )}
      <ToastContainer position="bottom-right" autoClose={5000} hideProgressBar={false} newestOnTop={false} closeOnClick rtl={false} pauseOnFocusLoss draggable pauseOnHover />
    </div>
  );
};

export default HomeGenieApp;