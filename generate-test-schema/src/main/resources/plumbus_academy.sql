DROP ALL OBJECTS;

create table person (
  id int primary key auto_increment,
  first_name text not null,
  last_name text not null,
  title text, // Intentionally nullable, as a person might not always have a title.
  birthday datetime not null
);

create table skill (
  id int primary key auto_increment,
  name text not null
);

create table person_skill (
  person_id int not null references person,
  skill_id int not null references skill,
  level int not null
);

insert into person values (default, 'Rick', 'Sanchez', 'Dr.', parsedatetime('1958/07/03', 'yyyy/MM/dd'));
// I don't actually know Morty's birthday, so don't go screaming at me.
insert into person values (default, 'Morty', 'Smith', null, parsedatetime('2003/02/19', 'yyyy/MM/dd'));
insert into skill values (default, 'Science');
insert into skill values (default, 'Martial Arts');
insert into skill values (default, 'Compassion');
insert into skill values (default, 'Self-Control');
insert into person_skill values (1, 1, 9001);
insert into person_skill values (1, 2, 10);
insert into person_skill values (1, 3, 0);
insert into person_skill values (1, 4, 1);
insert into person_skill values (2, 2, 3);
insert into person_skill values (2, 3, 7);
insert into person_skill values (2, 4, 3);
