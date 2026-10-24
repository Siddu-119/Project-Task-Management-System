import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { loginUser } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await login(email, password);
      loginUser(res.data);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed. Check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-logo">
          <h1>Project<span className="dot">Flow</span></h1>
          <p>Sign in to your workspace</p>
        </div>

        {error && <div className="alert alert-error">⚠ {error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              className="form-control"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              className="form-control"
              type="password"
              placeholder="Enter your password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', padding: '11px', marginTop: 8, fontSize: 15 }}
            disabled={loading}
          >
            {loading ? 'Signing in...' : 'Sign In →'}
          </button>
        </form>

        <div style={{ marginTop: 24, padding: 16, background: 'var(--bg3)', borderRadius: 8, fontSize: 13 }}>
          <div style={{ color: 'var(--text3)', marginBottom: 8, fontWeight: 600 }}>Demo Credentials</div>
          <div style={{ color: 'var(--text2)' }}>🔐 Admin: <code style={{color:'var(--accent)'}}>admin@example.com</code> / <code style={{color:'var(--accent)'}}>admin123</code></div>
          <div style={{ color: 'var(--text2)', marginTop: 4 }}>👤 Employee: <code style={{color:'var(--accent)'}}>alice@example.com</code> / <code style={{color:'var(--accent)'}}>alice123</code></div>
        </div>
      </div>
    </div>
  );
}
