CREATE TABLE users (
    id uuid NOT NULL,
    email varchar(254) NOT NULL,
    name varchar(20) NOT NULL,
    password text NOT NULL,
    is_admin boolean NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE blog_post (
    id uuid NOT NULL,
    title varchar(250) NOT NULL,
    subtitle varchar(250) NOT NULL,
    date date NOT NULL,
    body text NOT NULL,
    img_url varchar(250) NOT NULL,
    author uuid NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_blog_post PRIMARY KEY (id),
    CONSTRAINT uq_blog_post_title UNIQUE (title),
    CONSTRAINT fk_blog_post_author FOREIGN KEY (author) REFERENCES users (id)
);