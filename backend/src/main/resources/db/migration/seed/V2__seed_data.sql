-- Representative pizza/topping seed data, curated by hand.
-- Not a literal transcription of https://pizzapazzia.de/menu: that site renders
-- via client-side JS that could not be fetched at authoring time. Names,
-- ingredients and price level follow the master prompt's "no full 1:1 match
-- required" allowance (docs/Pizza_Shop_Master_Prompt.md §5) using standard
-- Italian pizzeria conventions typical of the German market. Swap in the real
-- menu text here if/when it's available.

INSERT INTO pizza (name, description, price, image_path, active, sort_order)
VALUES ('Margherita', 'Tomatensauce, Mozzarella, Basilikum', 7.50, NULL, TRUE, 10),
       ('Salami', 'Tomatensauce, Mozzarella, Salami', 8.50, NULL, TRUE, 20),
       ('Prosciutto', 'Tomatensauce, Mozzarella, gekochter Schinken', 8.90, NULL, TRUE, 30),
       ('Funghi', 'Tomatensauce, Mozzarella, Champignons', 8.50, NULL, TRUE, 40),
       ('Quattro Formaggi', 'Tomatensauce, Mozzarella, Gorgonzola, Parmesan, Emmentaler', 9.90, NULL, TRUE, 50),
       ('Diavola', 'Tomatensauce, Mozzarella, scharfe Salami, Peperoni', 9.50, NULL, TRUE, 60),
       ('Hawaii', 'Tomatensauce, Mozzarella, gekochter Schinken, Ananas', 8.90, NULL, TRUE, 70),
       ('Vegetaria', 'Tomatensauce, Mozzarella, Paprika, Champignons, Zwiebeln, Oliven', 9.20, NULL, TRUE, 80),
       ('Tonno', 'Tomatensauce, Mozzarella, Thunfisch, Zwiebeln', 9.50, NULL, TRUE, 90),
       ('Capricciosa', 'Tomatensauce, Mozzarella, Schinken, Champignons, Artischocken, Oliven', 10.50, NULL, TRUE, 100);

INSERT INTO topping (name, description, price, active)
VALUES ('Extra Käse', 'Zusätzliche Portion Mozzarella', 1.00, TRUE),
       ('Salami', 'Würzige Salami', 1.50, TRUE),
       ('Schinken', 'Gekochter Schinken', 1.50, TRUE),
       ('Champignons', 'Frische Champignons', 1.00, TRUE),
       ('Peperoni', 'Scharfe grüne Peperoni', 1.00, TRUE),
       ('Oliven', 'Schwarze Oliven', 1.00, TRUE),
       ('Zwiebeln', 'Frische Zwiebelringe', 0.80, TRUE),
       ('Ananas', 'Ananasstücke', 1.20, TRUE),
       ('Thunfisch', 'Thunfisch aus dem eigenen Saft', 1.80, TRUE),
       ('Rucola', 'Frischer Rucola', 1.20, TRUE);

-- Every topping is offerable on every seeded pizza; a real shop would curate
-- this per pizza, but a blanket assignment is a reasonable V1 default.
INSERT INTO pizza_topping (pizza_id, topping_id)
SELECT p.id, t.id
FROM pizza p
         CROSS JOIN topping t;
