--liquibase formatted sql

-- changeset ivan:1
INSERT INTO books (title, price, type) VALUES
('The Great Gatsby', 19.99, 'NEW_RELEASES'),
('1984', 15.50, 'REGULAR'),
('To Kill a Mockingbird', 9.99, 'OLD_EDITIONS'),
('Pride and Prejudice', 12.00, 'REGULAR'),
('The Catcher in the Rye', 14.75, 'NEW_RELEASES'),
('Brave NEW_RELEASES World', 11.25, 'OLD_EDITIONS'),
('The Hobbit', 20.00, 'NEW_RELEASES'),
('Fahrenheit 451', 13.50, 'REGULAR'),
('Jane Eyre', 10.99, 'OLD_EDITIONS'),
('Animal Farm', 16.00, 'NEW_RELEASES'),
('Lord of the Flies', 12.50, 'REGULAR'),
('The Odyssey', 8.99, 'OLD_EDITIONS'),
('Catch-22', 17.25, 'NEW_RELEASES'),
('Wuthering Heights', 11.75, 'REGULAR'),
('The Outsiders', 9.50, 'OLD_EDITIONS'),
('Dune', 22.00, 'NEW_RELEASES'),
('The Alchemist', 14.99, 'REGULAR'),
('Moby Dick', 10.25, 'OLD_EDITIONS'),
('Sapiens', 18.50, 'NEW_RELEASES'),
('The Road', 13.99, 'REGULAR');