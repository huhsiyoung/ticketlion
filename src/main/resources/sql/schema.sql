create table if not exists events (
                                      id bigserial primary key,
                                      title varchar(100) not null,
    start_at timestamp not null,
    category varchar(255) not null,
    venue varchar(200) not null,
    price integer not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    status varchar(20) not null check (status in ('ACTIVE', 'INACTIVE')),
    theme_color varchar(255) not null,
    thumbnail_emoji varchar(255) not null
    );

create table if not exists members (
                                       id uuid primary key,
                                       name varchar(80) not null,
    email varchar(120) not null,
    password_hash varchar(100) not null,
    phone varchar(20) not null,
    role varchar(20) not null check (role in ('ADMIN', 'USER')),
    created_at timestamp not null,
    updated_at timestamp not null
    );

create unique index if not exists idx_members_email
    on members(email);

create table if not exists seats (
                                     id bigserial primary key,
                                     event_id bigint not null,
                                     seat_no varchar(255) not null,
    status varchar(20) not null check (status in ('AVAILABLE', 'RESERVED')),
    constraint fk_seat_event
    foreign key (event_id) references events(id),
    constraint uk_event_seatno
    unique (event_id, seat_no)
    );

create index if not exists idx_seat_event
    on seats(event_id);

create index if not exists idx_seat_status
    on seats(status);

create table if not exists reservations (
                                            id bigserial primary key,
                                            seat_id bigint not null,
                                            user_id uuid not null,
                                            reserved_at timestamp not null,
                                            status varchar(20) not null check (status in ('PENDING', 'CONFIRMED', 'CANCELLED')),
    constraint fk_reservation_seat
    foreign key (seat_id) references seats(id),
    constraint fk_reservation_member
    foreign key (user_id) references members(id)
    );

create index if not exists idx_reservation_seat
    on reservations(seat_id);

create index if not exists idx_reservation_user
    on reservations(user_id);

create table if not exists payments (
                                        id bigserial primary key,
                                        reservation_id bigint not null,
                                        user_id uuid not null,
                                        amount bigint not null,
                                        status varchar(20) not null check (status in ('READY', 'SUCCESS', 'FAILED')),
    idempotency_key varchar(64) not null,
    provider varchar(255) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_payment_idempotency
    unique (idempotency_key),
    constraint fk_payment_reservation
    foreign key (reservation_id) references reservations(id),
    constraint fk_payment_member
    foreign key (user_id) references members(id)
    );

create index if not exists idx_payment_reservation
    on payments(reservation_id);

create index if not exists idx_payment_user
    on payments(user_id);

INSERT INTO events
(title, start_at, category, venue, price, theme_color, thumbnail_emoji, created_at, updated_at, status)
VALUES
    ('BTS 월드투어 인 서울', TIMESTAMP '2025-04-15 19:00:00', 'K-POP', '잠실 올림픽 주경기장', 110000, 'g1', '🎤', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('아이유 이벤트 <The Winning>', TIMESTAMP '2025-03-22 18:00:00', 'K-POP', 'KSPO DOME', 132000, 'g2', '🎵', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Ed Sheeran Asia Tour 2025', TIMESTAMP '2025-05-10 19:30:00', 'POP', '고척 스카이돔', 154000, 'g3', '🎸', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('임영웅 전국투어 <IM HERO>', TIMESTAMP '2025-06-14 19:00:00', 'K-POP', '올림픽공원 체조경기장', 143000, 'g4', '🎤', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Coldplay Music of the Spheres', TIMESTAMP '2025-04-28 20:00:00', 'POP', '인천 아시아드 주경기장', 165000, 'g5', '🌟', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('2025 봄 재즈 페스티벌', TIMESTAMP '2025-05-01 17:00:00', '재즈', '올림픽공원 88잔디마당', 99000, 'g6', '🎺', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('세븐틴 <FOLLOW AGAIN> 투어', TIMESTAMP '2025-03-29 18:00:00', 'K-POP', '서울 월드컵경기장', 121000, 'g7', '💎', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Bruno Mars 24K Magic Tour', TIMESTAMP '2025-07-05 19:00:00', 'POP', 'KSPO DOME', 176000, 'g8', '🕺', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('뉴진스 팬미팅 <Bunnies>', TIMESTAMP '2025-04-05 18:00:00', 'K-POP', '올림픽공원 체조경기장', 110000, 'g9', '🐰', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('John Mayer Asia Tour', TIMESTAMP '2025-05-20 20:00:00', 'POP', '블루스퀘어 마스터카드홀', 143000, 'g10', '🎸', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('에픽하이 2025 이벤트', TIMESTAMP '2025-03-15 19:00:00', '힙합/R&B', '올림픽홀', 99000, 'g11', '🎤', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Day6 3rd World Tour', TIMESTAMP '2025-04-12 19:00:00', 'K-POP', 'YES24 라이브홀', 88000, 'g12', '🎸', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE');

INSERT INTO seats (event_id, seat_no, status)
SELECT e.event_id,
       r.row_letter || s.num,
       'AVAILABLE'
FROM (
         VALUES
             (1),(2),(3),(4),(5),(6),
             (7),(8),(9),(10),(11),(12)
     ) AS e(event_id)
         CROSS JOIN (
    VALUES ('A'),('B'),('C'),('D'),('E'),('F'),('G'),('H')
) r(row_letter)
         CROSS JOIN (
    SELECT generate_series(1,10) AS num
) s;