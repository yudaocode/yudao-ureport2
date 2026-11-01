DROP TABLE IF EXISTS employee;
CREATE TABLE employee (id INT PRIMARY KEY, dept_code VARCHAR(10), employee_name VARCHAR(50), salary DECIMAL(10,2));
INSERT INTO employee VALUES (1, 'D01', 'JdbcEmpAlice', 1000.00);
INSERT INTO employee VALUES (2, 'D01', 'JdbcEmpBob', 2000.00);
INSERT INTO employee VALUES (3, 'D02', 'JdbcEmpCarol', 3500.50);
