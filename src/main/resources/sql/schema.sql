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
    thumbnail_emoji varchar(255) not null,
    description text
    );

create table if not exists members (
                                       id uuid primary key,
                                       name varchar(80) not null,
    username varchar(50) not null,
    password_hash varchar(100) not null,
    role varchar(20) not null check (role in ('ADMIN', 'USER')),
    created_at timestamp not null,
    updated_at timestamp not null
    );

create unique index if not exists idx_members_username
    on members(username);

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
(title, start_at, category, venue, price, theme_color, thumbnail_emoji, description, created_at, updated_at, status)
VALUES
    ('BTS 월드투어 인 서울', TIMESTAMP '2027-04-15 19:00:00', 'K-POP', '잠실 올림픽 주경기장', 110000, 'g1', '🎤', '전 세계를 열광시킨 BTS의 월드투어가 드디어 서울에 상륙한다. 화려한 무대 연출과 히트곡 라인업으로 꽉 채운 시간, 팬들과 함께 만드는 특별한 밤.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('아이유 이벤트 <The Winning>', TIMESTAMP '2027-03-22 18:00:00', 'K-POP', 'KSPO DOME', 132000, 'g2', '🎵', '아이유가 준비한 스페셜 콘서트 <The Winning>. 데뷔부터 지금까지의 여정을 담은 셋리스트와 섬세한 라이브로 관객을 사로잡는다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Ed Sheeran Asia Tour 2025', TIMESTAMP '2027-05-10 19:30:00', 'POP', '고척 스카이돔', 154000, 'g3', '🎸', '기타 하나로 스타디움을 가득 채우는 에드 시런의 아시아 투어. 루프 페달로 완성하는 독보적인 라이브 사운드를 직접 확인할 수 있다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('임영웅 전국투어 <IM HERO>', TIMESTAMP '2027-06-14 19:00:00', 'K-POP', '올림픽공원 체조경기장', 143000, 'g4', '🎤', '트로트 열풍의 중심, 임영웅의 전국투어 <IM HERO>. 폭넓은 세대를 아우르는 감성 보컬과 팬서비스로 채워지는 무대.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Coldplay Music of the Spheres', TIMESTAMP '2027-04-28 20:00:00', 'POP', '인천 아시아드 주경기장', 165000, 'g5', '🌟', '콜드플레이의 대표 투어 ''Music of the Spheres''. LED 팔찌와 대형 비주얼 연출이 만들어내는 압도적인 스타디움 쇼.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('2025 봄 재즈 페스티벌', TIMESTAMP '2027-05-01 17:00:00', '재즈', '올림픽공원 88잔디마당', 99000, 'g6', '🎺', '봄밤을 채우는 국내외 재즈 아티스트들의 무대. 잔디밭에 자리를 깔고 즐기는 편안한 야외 공연.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('세븐틴 <FOLLOW AGAIN> 투어', TIMESTAMP '2027-03-29 18:00:00', 'K-POP', '서울 월드컵경기장', 121000, 'g7', '💎', '13인 13색 세븐틴의 <FOLLOW AGAIN> 투어. 칼군무와 폭발적인 에너지로 완성하는 퍼포먼스 무대.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Bruno Mars 24K Magic Tour', TIMESTAMP '2027-07-05 19:00:00', 'POP', 'KSPO DOME', 176000, 'g8', '🕺', '그루브의 정석, 브루노 마스의 ''Bruno Mars 24K Magic Tour''. 화려한 밴드 연주와 댄스로 채우는 펑키한 무대.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('뉴진스 팬미팅 <Bunnies>', TIMESTAMP '2027-04-05 18:00:00', 'K-POP', '올림픽공원 체조경기장', 110000, 'g9', '🐰', '뉴진스와 함께하는 스페셜 팬미팅 <Bunnies>. 토크, 미니게임, 라이브 무대까지 알찬 구성으로 준비했다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('John Mayer Asia Tour', TIMESTAMP '2027-05-20 20:00:00', 'POP', '블루스퀘어 마스터카드홀', 143000, 'g10', '🎸', '블루스와 팝을 넘나드는 존 메이어의 기타 라이브. 감각적인 즉흥 연주가 돋보이는 아시아 투어.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('에픽하이 2025 이벤트', TIMESTAMP '2027-03-15 19:00:00', '힙합/R&B', '올림픽홀', 99000, 'g11', '🎤', '타블로, 미쓰라진, 투컷의 에픽하이가 선사하는 힙합 라이브. 시대를 관통하는 가사와 묵직한 사운드.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Day6 3rd World Tour', TIMESTAMP '2027-04-12 19:00:00', 'K-POP', 'YES24 라이브홀', 88000, 'g12', '🎸', '밴드 사운드의 진수를 보여주는 데이식스의 월드투어. 직접 연주하는 라이브 밴드만의 생생함이 매력.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    -- J-POP
    ('ZUTOMAYO Live Tour 2027', TIMESTAMP '2027-08-14 19:00:00', 'J-POP', '올림픽공원 SK핸드볼경기장', 143000, 'g1', '🌙', '정체를 숨긴 채 독창적인 음악 세계를 펼치는 즛토마요의 라이브 투어. 몽환적인 영상 연출과 개성 있는 사운드가 특징.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Vaundy Asia Tour 2027', TIMESTAMP '2027-09-05 19:00:00', 'J-POP', 'YES24 라이브홀', 132000, 'g5', '🎧', '장르를 자유롭게 넘나드는 신세대 아티스트 바운디의 아시아 투어. 세련된 사운드 프로듀싱을 라이브로 만나본다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Ado Special Live in Seoul', TIMESTAMP '2027-10-02 19:00:00', 'J-POP', 'KSPO DOME', 165000, 'g11', '🎭', '폭발적인 가창력의 아도가 선사하는 스페셜 라이브. 강렬한 비주얼 연출과 압도적인 보컬이 무대를 채운다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Tuki. Asia Tour 2027', TIMESTAMP '2027-11-08 19:00:00', 'J-POP', '블루스퀘어 마스터카드홀', 110000, 'g6', '✨', '화제의 신예 아티스트 츠키의 아시아 투어. 감성적인 어쿠스틱 사운드와 담백한 목소리가 매력.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Aimyon Live in Seoul', TIMESTAMP '2027-07-18 18:00:00', 'J-POP', '올림픽공원 88잔디마당', 121000, 'g4', '🎸', '일상의 감정을 노래하는 싱어송라이터 아이묭의 서울 라이브. 담담하지만 깊은 울림을 주는 무대.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    -- 내한 (US POP)
    ('Ariana Grande Eternal Sunshine Tour', TIMESTAMP '2027-06-20 19:30:00', 'POP', '고척 스카이돔', 187000, 'g7', '🎀', '팝의 아이콘 아리아나 그란데의 ''Eternal Sunshine Tour''. 폭넓은 음역대와 화려한 무대 연출이 어우러지는 공연.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Charlie Puth Asia Tour 2027', TIMESTAMP '2027-08-29 19:00:00', 'POP', 'KSPO DOME', 143000, 'g3', '🎹', '멀티 악기 연주와 감미로운 보컬의 찰리 푸스, 아시아 투어로 한국을 찾는다. 프로듀서 출신다운 섬세한 라이브 편곡이 돋보인다.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Post Malone Live in Seoul', TIMESTAMP '2027-09-19 19:00:00', 'POP', '잠실 올림픽 주경기장', 165000, 'g12', '🔥', '장르를 넘나드는 힙합 아이콘 포스트 말론의 첫 내한 라이브. 록적인 사운드와 힙합이 뒤섞인 독특한 무대.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE'),

    ('Justin Bieber World Tour 2027', TIMESTAMP '2027-10-24 19:00:00', 'POP', '인천 아시아드 주경기장', 176000, 'g2', '🎤', '팝스타 저스틴 비버의 월드투어가 한국을 찾는다. 커리어를 아우르는 히트곡 무대와 화려한 퍼포먼스로 채워지는 밤.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'ACTIVE');

INSERT INTO seats (event_id, seat_no, status)
SELECT e.event_id,
       r.row_letter || s.num,
       'AVAILABLE'
FROM (
         VALUES
             (1),(2),(3),(4),(5),(6),
             (7),(8),(9),(10),(11),(12),
             (13),(14),(15),(16),(17),(18),(19),(20),(21)
     ) AS e(event_id)
         CROSS JOIN (
    VALUES ('A'),('B'),('C'),('D'),('E'),('F'),('G'),('H')
) r(row_letter)
         CROSS JOIN (
    SELECT generate_series(1,10) AS num
) s;