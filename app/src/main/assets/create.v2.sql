BEGIN;
CREATE TABLE "android_metadata" ("locale" TEXT DEFAULT 'en_US');
INSERT INTO "android_metadata" VALUES ('en_US');

CREATE TABLE location(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
main_db_id INTEGER DEFAULT 0,
lat TEXT,
long TEXT,
accuracy TEXT,
user_id INTEGER REFERENCES users(_id) ON UPDATE CASCADE
);

--INSERT INTO location(lat, long, accuracy, user_id) VALUES('45.8223098', '16.1281997', '23.0', 1);
--INSERT INTO location(lat, long, accuracy, user_id) VALUES('45.8222000', '16.1281800', '23.0', 1);
--INSERT INTO location(lat, long, accuracy, user_id) VALUES('45.8228000', '16.1281500', '23.0', 1);

CREATE TABLE photo(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
main_db_id INTEGER DEFAULT 0,
is_outdated BOOLEAN DEFAULT 'N',
time_taken TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
name TEXT,
user_id INTEGER REFERENCES users(_id) ON UPDATE CASCADE,
location_id INTEGER REFERENCES location(_id) ON UPDATE CASCADE
);

--INSERT INTO photo(user_id, location_id, name) VALUES(1, 1);

CREATE TABLE note(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
main_db_id INTEGER DEFAULT 0,
content TEXT,
time_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
user_id INTEGER REFERENCES users(_id) ON UPDATE CASCADE
);

--INSERT INTO note(content, user_id) VALUES('Lorem ipsum dolor sit amet, consectetur adipiscing elit. In condimentum elit ac turpis fringilla luctus. Cras posuere tellus sed magna commodo, scelerisque mollis enim hendrerit. In fermentum sodales iaculis. Duis eu ipsum vestibulum, vestibulum metus non, eleifend ante. Sed eleifend malesuada ligula, vel placerat mi ultricies id. In auctor molestie massa quis ultricies. Nulla sodales tellus sed nulla adipiscing, quis sagittis quam aliquam. Suspendisse convallis enim commodo lobortis euismod. Sed sed ante vitae diam fringilla placerat. Phasellus vitae eleifend urna. Donec posuere eu lacus at aliquam. Suspendisse tempus diam leo, scelerisque varius lectus auctor vitae. Cras non pretium nisi, malesuada porttitor turpis. Curabitur aliquet dui tempus dignissim hendrerit. Quisque sodales semper arcu, at venenatis purus auctor at.', 1);

CREATE TABLE settings(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
main_db_id INTEGER DEFAULT 0,
time_to_next_update INTEGER DEFAULT 60,
min_accuracy INTEGER DEFAULT 200
);

CREATE TABLE tree(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
main_db_id INTEGER DEFAULT 0,
time_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
time_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
time_for_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
three_digit_number INTEGER,
location_id INTEGER REFERENCES location(_id) ON UPDATE CASCADE,
is_missing BOOLEAN DEFAULT 'N',
is_synced BOOLEAN DEFAULT 'N',
is_priority BOOLEAN DEFAULT 'N',
cause_of_death_id INTEGER REFERENCES note(_id) ON UPDATE CASCADE,
settings_id INTEGER REFERENCES settings(_id) ON UPDATE CASCADE,
settings_override_id INTEGER REFERENCES settings(_id) ON UPDATE CASCADE,
user_id INTEGER REFERENCES users(_id) ON UPDATE CASCADE,
planter_identification_id INTEGER REFERENCES planter_identifications(_id) ON UPDATE CASCADE
);

CREATE TABLE planter_identifications(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
planter_details_id INTEGER,
identifier TEXT,
photo_path TEXT,
photo_url TEXT,
time_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE planter_details(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
identifier TEXT,
first_name TEXT,
last_name TEXT,
organization TEXT,
phone TEXT,
email TEXT,
uploaded BOOLEAN DEFAULT 'N',
time_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--INSERT INTO tree(location_id, cause_of_death_id, settings_id, settings_override_id, user_id) VALUES(1, null, 1, 1, 1);

CREATE TABLE pending_updates(
_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
main_db_id INTEGER DEFAULT 0,
user_id INTEGER REFERENCES users(_id) ON UPDATE CASCADE,
settings_id INTEGER REFERENCES settings(_id) ON UPDATE CASCADE,
tree_id INTEGER REFERENCES tree(_id) ON UPDATE CASCADE,
location_id INTEGER REFERENCES location(_id) ON UPDATE CASCADE,
radius INTEGER
);


CREATE TABLE tree_photo(
tree_id INTEGER REFERENCES tree(_id) ON UPDATE CASCADE,
photo_id INTEGER REFERENCES photo(_id) ON UPDATE CASCADE
);

--INSERT INTO tree_photo(photo_id, tree_id) VALUES(1, 1);

CREATE TABLE tree_note(
tree_id INTEGER REFERENCES tree(_id) ON UPDATE CASCADE,
note_id INTEGER REFERENCES note(_id) ON UPDATE CASCADE
);

--INSERT INTO tree_note(note_id, tree_id) VALUES(1, 1);

CREATE TABLE global_settings(
	settings_id INTEGER REFERENCES settings(_id) ON UPDATE CASCADE
);

INSERT INTO global_settings(settings_id) VALUES(1);



COMMIT;
