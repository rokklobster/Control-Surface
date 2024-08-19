create table if not exists users (
	id integer primary key not null,
    token text not null
);

--cmdcut

create table if not exists registrations (
    userId integer not null,
    serverName text not null,
    pushUrl text not null,
    cancelUrl text not null,
    queryUrl text not null,
    primary key (userId, serverName),
    foreign key (userId) references users(id)
);