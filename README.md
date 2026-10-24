# ProjectFlow V2.0 — Project & Task Management System

A full-stack mini project built with **Spring Boot** (backend) and **React.js** (frontend).

---

## Tech Stack

| Layer     | Technology                        |
|-----------|-----------------------------------|
| Frontend  | React.js, React Router, Axios, Plain CSS |
| Backend   | Spring Boot 3.2, Spring Data JPA  |
| Database  | MySQL 8                           |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+ & npm
- MySQL 8

---

## Step 1 — Database Setup

Open MySQL and run the contents of:

```
backend/src/main/resources/data.sql
```

This creates the `project_management` database, all tables, and inserts sample data.

**Upgrading from V1?** Back up, then run the V2 migration on your existing database (default `ptms`):
```
mysql -u root -p ptms < backend/src/main/resources/v2_migration.sql
```
It is safe to run more than once and keeps all existing data.

**Database password** is read from the `DB_PASSWORD` environment variable (nothing secret is stored in the repo):
```
# Windows PowerShell
$env:DB_PASSWORD="your_mysql_password"
# Mac/Linux
export DB_PASSWORD='your_mysql_password'
```
Optional: `DB_USERNAME`, `DB_URL`, `JWT_SECRET` (set a long random JWT_SECRET for real deployments).

---

## Step 2 — Start Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

Backend starts at: **http://localhost:8080**

---

## Step 3 — Start Frontend (React)

```bash
cd frontend
npm install
npm start
```

Frontend starts at: **http://localhost:3000**

---

## Demo Credentials

| Role     | Email                  | Password   |
|----------|------------------------|------------|
| Admin    | admin@example.com      | admin123   |
| Employee | alice@example.com      | alice123   |
| Employee | bob@example.com        | bob123     |
| Employee | carol@example.com      | carol123   |
| Employee | david@example.com      | david123   |

---

## V2.0 Features

Dashboard with real statistics and charts · Project details page with progress bar and activity ·
Kanban board (drag & drop) · Team management and workload · Task priorities and due dates ·
Task comments · Notifications (stored in MySQL) · Search and filters · Reports (admin) ·
JWT login with BCrypt passwords · Server-side validation · Responsive UI.

New endpoints: `/api/dashboard`, `/api/reports`, `/api/notifications`, `/api/projects/{id}/members`,
`/api/projects/{id}/activity`, `/api/projects/{id}/tasks`, `/api/tasks/{id}/comments`,
`/api/tasks?search=&status=&priority=&assignedToId=&projectId=&dueFrom=&dueTo=&overdue=`,
`/api/users/me`, `/api/users/me/password`. All endpoints except `/api/auth/login` need a JWT token.

## V1 Features (still supported)

### Admin
- Dashboard with project/task stats and progress bars
- Create, edit, delete Projects
- Create, edit, delete Tasks — assign to employees & projects
- View all tasks with status filtering

### Employee
- Dashboard showing personal task summary
- View own assigned tasks only
- Update task status: **Pending → In Progress → Completed**

---

## API Endpoints

| Method | Endpoint                     | Description              |
|--------|------------------------------|--------------------------|
| POST   | /api/auth/login              | User login               |
| GET    | /api/projects                | Get all projects         |
| POST   | /api/projects                | Create project           |
| PUT    | /api/projects/{id}           | Update project           |
| DELETE | /api/projects/{id}           | Delete project           |
| GET    | /api/tasks                   | Get all tasks            |
| GET    | /api/tasks/user/{userId}     | Get tasks by user        |
| GET    | /api/tasks/project/{id}      | Get tasks by project     |
| POST   | /api/tasks                   | Create task              |
| PUT    | /api/tasks/{id}              | Update task              |
| PATCH  | /api/tasks/{id}/status       | Update task status       |
| DELETE | /api/tasks/{id}              | Delete task              |
| GET    | /api/users/employees         | List all employees       |

---

## Project Structure

```
project-management/
├── backend/                  # Spring Boot
│   ├── src/main/java/com/projectmgmt/
│   │   ├── model/           # User, Project, Task entities
│   │   ├── repository/      # JPA repositories
│   │   ├── service/         # Business logic
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   └── config/          # CORS config
│   └── src/main/resources/
│       ├── application.properties
│       └── data.sql         # DB setup script
│
└── frontend/                 # React.js
    └── src/
        ├── pages/           # LoginPage, AdminDashboard, etc.
        ├── components/      # Layout (sidebar + topbar)
        ├── services/        # Axios API calls
        ├── context/         # AuthContext
        └── styles/          # global.css
```
