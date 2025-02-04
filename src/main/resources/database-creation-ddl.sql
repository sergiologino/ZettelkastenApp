-- DROP SCHEMA public;

--CREATE SCHEMA public IF NOT EXIST AUTHORIZATION pg_database_owner;

--COMMENT ON SCHEMA public IS 'standard public schema';
-- public.projects определение

-- Drop table

-- DROP TABLE public.projects;

CREATE TABLE  IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    color_theme boolean NOT null,
    tlg_username VARCHAR(255),
    phone_number VARCHAR(255),
    billing boolean DEFAULT FALSE,
    avatar bytea,
    telegram_chat_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE  IF NOT EXISTS public.projects (
	id uuid NOT NULL,
	description varchar(500) NULL,
	"name" varchar(255) NOT NULL,
	user_id UUID NOT NULL,
	сreated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	"position" int8,
	color varchar(255),
	is_default boolean DEFAULT FALSE,
	CONSTRAINT projects_pkey PRIMARY KEY (id),
	CONSTRAINT uk1e447b96pedrvtxw44ot4qxem UNIQUE (user_id, name),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- public.tags определение

-- Drop table

-- DROP TABLE public.tags;

CREATE TABLE  IF NOT EXISTS public.tags (
	id uuid NOT NULL,
	is_auto_generated bool NOT NULL,
	"name" varchar(255) NOT NULL,
	user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT tags_pkey PRIMARY KEY (id),
	CONSTRAINT ukt48xdq560gs3gap9g7jg36kgc UNIQUE (name),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- public.notes определение

-- Drop table

-- DROP TABLE public.notes;

CREATE TABLE  IF NOT EXISTS public.notes (
	id uuid NOT NULL,
	title VARCHAR(255),
	ai_summary bool NULL,
	annotation varchar(255) NULL,
	audio_file_path varchar(255) NULL,
	"content" varchar(255) NOT NULL,
	file_path varchar(255) NULL,
	file_type varchar(255) NULL,
	neural_network varchar(255) NULL,
	recognized_text varchar(255) NULL,
	note_url varchar(255) NULL,
	project_id uuid NULL,
	should_analyze bool NOT NULL,
	position_x int8 NULL,
	position_y int8 NULL,
	width int8 NULL,
	height int8 NULL,
	user_id UUID NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT notes_pkey PRIMARY KEY (id),
	CONSTRAINT fkf5kwkuxo55mgr2vkluhrh7tth FOREIGN KEY (project_id) REFERENCES public.projects(id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);


-- public.open_graph_data определение

-- Drop table

-- DROP TABLE public.open_graph_data;

CREATE TABLE  IF NOT EXISTS public.open_graph_data (
	id uuid NOT NULL,
	description varchar(255) NULL,
	image varchar(255) NULL,
	title varchar(255) NULL,
	url varchar(255) NOT NULL,
	note_id uuid NOT NULL,
	user_id UUID ,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT open_graph_data_pkey PRIMARY KEY (id),
	CONSTRAINT uk1cuhcqmq1m00fq38i7annn7ek UNIQUE (note_id, url),
	CONSTRAINT unique_note_url UNIQUE (note_id, url),
	CONSTRAINT fk7c8yqk0e2ae540myuarcun98p FOREIGN KEY (note_id) REFERENCES public.notes(id),
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- public.note_audios определение

-- Drop table

-- DROP TABLE public.note_audios;

CREATE TABLE  IF NOT EXISTS public.note_audios (
	id uuid NOT NULL,
	server_file_path varchar(255) NOT NULL,
	original_name varchar(255) NOT NULL,
	note_id uuid NOT NULL,
	user_id UUID ,
	audio_type varchar(255),
	audio_size NUMERIC,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT note_audios_pkey PRIMARY KEY (id),
	CONSTRAINT note_audios_note_id_fkey FOREIGN KEY (note_id) REFERENCES public.notes(id) ON DELETE cascade,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);


-- public.note_files определение

-- Drop table

-- DROP TABLE public.note_files;

CREATE TABLE  IF NOT EXISTS public.note_files (
	id uuid NOT NULL,
	server_file_path varchar(255) NOT NULL,
	original_name varchar(255) NOT NULL,
	note_id uuid NOT NULL,
	user_id UUID ,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT note_files_pkey PRIMARY KEY (id),
	CONSTRAINT note_files_note_id_fkey FOREIGN KEY (note_id) REFERENCES public.notes(id) ON DELETE cascade,
	FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE cascade
);


-- public.note_tags определение

-- Drop table

-- DROP TABLE public.note_tags;

CREATE TABLE  IF NOT exists public.note_tags (
	note_id uuid NOT NULL,
	tag_id uuid NOT NULL,
	PRIMARY KEY (note_id, tag_id),
	CONSTRAINT fk8babdwu6uqiu4rdkeuy8dkna0 FOREIGN KEY (tag_id) REFERENCES public.tags(id),
	CONSTRAINT fkb15yxop81senc5xs5tjrsy4k4 FOREIGN KEY (note_id) REFERENCES public.notes(id),
	FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_notes ON notes(user_id);
CREATE INDEX idx_user_projects ON projects(user_id);
CREATE INDEX idx_user_tags ON tags(user_id);
