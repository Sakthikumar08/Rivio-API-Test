# Rivio HRMS — REST Assured API Automation Suite

## Credentials (from seed SQL)

All users seeded with password `password123`.

| User | Email | Role | Emp Profile |
|------|-------|------|-------------|
| Admin | admin@rivio.com | Super Admin | — |
| Sarah | sarah.hr@rivio.com | HR Manager | 2 |
| John | john.manager@rivio.com | Department Head | 1 |
| Alice | alice.emp@rivio.com | Employee | 3 |
| Bob | bob.emp@rivio.com | Employee | 4 |

**No changes needed** — `config.properties` already has `password123`.

## Running Tests

```bash
mvn clean test          # Full suite
mvn allure:serve        # Open HTML report
```

## Seeded Data Highlights Used in Tests

| Module | Seeded IDs used |
|--------|----------------|
| Locations | 1=Bengaluru HQ, 2=Mumbai, 3=New York |
| Departments | 1=Engineering, 2=HR, 3=Sales |
| Designations | 1-5 (VP, Sr SE, FE Dev, HR Dir, Sales Exec) |
| Employees | 1=John, 2=Sarah, 3=Alice, 4=Bob |
| Leave Types | 1=Sick(12d), 2=Casual(10d), 3=Earned(15d) |
| Leave Requests | id=1 Alice APPROVED, id=2 Bob PENDING |
| Pay Cycles | id=1 Feb 2026 PAID, id=2 Mar 2026 PROCESSING |
| Payslips | id=1 John 138200, id=2 Alice 83200 |
| Job Openings | id=1 Lead Backend (OPEN), id=2 Sales Mgr (ON_HOLD) |
| Candidates | id=1 Charlie Brown INTERVIEWING, id=2 Diana Prince OFFERED |
| Work Days | 1-5 working=true, 6-7 working=false |
| Holidays | 1=New Year, 2=Republic Day, 3=Labour Day |
