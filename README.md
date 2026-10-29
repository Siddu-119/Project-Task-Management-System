# 🚀 Project & Task Management System

> Plan projects. Manage tasks. Track progress.

A full-stack web application designed to simplify project management, task assignment, and progress tracking. The system provides role-based access for administrators and employees, helping teams organize projects and manage their daily work efficiently.

## ✨ Features & Highlights

### 🔐 Authentication & Authorization
- User authentication and secure password handling.
- JWT-based authentication.
- Role-based access for administrators and employees.
- Protected API endpoints.

### 📋 Project Management
- Create and manage projects.
- Organize project information and team members.
- Track project progress.
- Manage project participation.

### ✅ Task Management
- Create and assign tasks.
- Track task status and progress.
- Organize tasks within projects.
- Support task updates and management.

### 👥 User Management
- Manage registered users.
- Separate administrator and employee permissions.
- Associate team members with projects.

### 💻 User Interface
- React-based frontend.
- Responsive layouts using custom CSS.
- Reusable components and React Hooks.
- API integration using Axios.

## 🛠️ Tech Stack

### Frontend
- React.js
- JavaScript
- HTML5
- CSS3
- Axios

### Backend
- Java
- Spring Boot
- Spring Security
- REST APIs
- JWT Authentication
- Maven

### Database
- MySQL

### Development Tools
- Visual Studio Code
- Git
- GitHub
- Postman

## 🏗️ Application Architecture

The application follows a full-stack architecture, separating the frontend, backend, and database.

```text
project-management-v2/
├── frontend/
│   └── React application
├── backend/
│   └── Spring Boot application
├── database/
│   └── Database scripts (if included)
└── README.md
```

## 🔄 How It Works

1. Users log in to the application.
2. The system identifies the user's role.
3. Administrators manage projects, users, and tasks according to their permissions.
4. Employees access their assigned work.
5. Task updates help track project progress.
6. The frontend communicates with the backend through REST APIs.
7. The backend processes requests and interacts with the MySQL database.

## 🚀 Getting Started

### Prerequisites

Install the following tools:

- Java Development Kit (JDK)
- Maven
- Node.js and npm
- MySQL Server
- Git

### 1. Clone the Repository

```bash
git clone https://github.com/Siddu-119/project-management-v2.git
cd project-management-v2
```

### 2. Configure the Database

Create a MySQL database for the application and configure the database connection details in the Spring Boot configuration file.

Never commit database passwords, JWT secrets, or other sensitive credentials to GitHub.

### 3. Start the Backend

Open a terminal in the backend directory:

```bash
cd backend
```

Run the application using the Maven wrapper if included:

**Windows**
```bash
mvnw.cmd spring-boot:run
```

**Linux/macOS**
```bash
./mvnw spring-boot:run
```

If the project does not include a Maven wrapper, use the installed Maven command:

```bash
mvn spring-boot:run
```

### 4. Start the Frontend

Open a separate terminal in the frontend directory:

```bash
cd frontend
npm install
```

Start the development server using the script defined in `package.json`:

```bash
npm start
```

If the project uses Vite instead of Create React App, use:

```bash
npm run dev
```

Open the local URL displayed in your terminal.

## 🎯 Project Objective

To build a practical project management platform that helps teams organize projects, assign responsibilities, and track task progress through a centralized application.

## 📚 Key Learning Outcomes

- Full-stack web application development.
- Building REST APIs with Spring Boot.
- Frontend development using React.js.
- Database integration using MySQL.
- Authentication and authorization with JWT and Spring Security.
- Connecting frontend applications to backend services.
- Managing source code with Git and GitHub.

## 👨‍💻 Developer

**Siddu B R**

Engineering Student | Aspiring Full-Stack Developer

GitHub: [Siddu-119](https://github.com/Siddu-119)

## 🤝 Contributions

Suggestions, feedback, and improvements are welcome.

---

⭐ If you find this project interesting, consider starring the repository.
