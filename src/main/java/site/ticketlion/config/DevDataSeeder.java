package site.ticketlion.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;
import site.ticketlion.domain.Seat;
import site.ticketlion.domain.SeatStatus;
import site.ticketlion.repository.EventRepository;
import site.ticketlion.repository.SeatRepository;

/**
 * dev 프로파일 전용 초기 데이터 시더.
 * <p>
 * {@code src/main/resources/sql/schema.sql}에 있는 운영(prod) 시드 데이터와 동일한
 * 이벤트/좌석 목록을 H2 dev DB에 채워 넣는다. 이미 이벤트가 하나라도 있으면 아무 것도 하지 않는다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final int ROWS = 8;   // A~H
    private static final int COLS = 10;  // 1~10

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (eventRepository.count() > 0) {
            log.info("[dev-seed] events가 이미 존재해 시드 데이터를 건너뜁니다.");
            return;
        }

        List<Event> events = List.of(
            event("BTS 월드투어 인 서울", LocalDateTime.of(2027, 4, 15, 19, 0), "K-POP", "잠실 올림픽 주경기장", 110000, "g1", "🎤",
                "전 세계를 열광시킨 BTS의 월드투어가 드디어 서울에 상륙한다. 화려한 무대 연출과 히트곡 라인업으로 꽉 채운 시간, 팬들과 함께 만드는 특별한 밤."),
            event("아이유 이벤트 <The Winning>", LocalDateTime.of(2027, 3, 22, 18, 0), "K-POP", "KSPO DOME", 132000, "g2", "🎵",
                "아이유가 준비한 스페셜 콘서트 <The Winning>. 데뷔부터 지금까지의 여정을 담은 셋리스트와 섬세한 라이브로 관객을 사로잡는다."),
            event("Ed Sheeran Asia Tour 2025", LocalDateTime.of(2027, 5, 10, 19, 30), "POP", "고척 스카이돔", 154000, "g3", "🎸",
                "기타 하나로 스타디움을 가득 채우는 에드 시런의 아시아 투어. 루프 페달로 완성하는 독보적인 라이브 사운드를 직접 확인할 수 있다."),
            event("임영웅 전국투어 <IM HERO>", LocalDateTime.of(2027, 6, 14, 19, 0), "K-POP", "올림픽공원 체조경기장", 143000, "g4", "🎤",
                "트로트 열풍의 중심, 임영웅의 전국투어 <IM HERO>. 폭넓은 세대를 아우르는 감성 보컬과 팬서비스로 채워지는 무대."),
            event("Coldplay Music of the Spheres", LocalDateTime.of(2027, 4, 28, 20, 0), "POP", "인천 아시아드 주경기장", 165000, "g5", "🌟",
                "콜드플레이의 대표 투어 'Music of the Spheres'. LED 팔찌와 대형 비주얼 연출이 만들어내는 압도적인 스타디움 쇼."),
            event("2025 봄 재즈 페스티벌", LocalDateTime.of(2027, 5, 1, 17, 0), "재즈", "올림픽공원 88잔디마당", 99000, "g6", "🎺",
                "봄밤을 채우는 국내외 재즈 아티스트들의 무대. 잔디밭에 자리를 깔고 즐기는 편안한 야외 공연."),
            event("세븐틴 <FOLLOW AGAIN> 투어", LocalDateTime.of(2027, 3, 29, 18, 0), "K-POP", "서울 월드컵경기장", 121000, "g7", "💎",
                "13인 13색 세븐틴의 <FOLLOW AGAIN> 투어. 칼군무와 폭발적인 에너지로 완성하는 퍼포먼스 무대."),
            event("Bruno Mars 24K Magic Tour", LocalDateTime.of(2027, 7, 5, 19, 0), "POP", "KSPO DOME", 176000, "g8", "🕺",
                "그루브의 정석, 브루노 마스의 'Bruno Mars 24K Magic Tour'. 화려한 밴드 연주와 댄스로 채우는 펑키한 무대."),
            event("뉴진스 팬미팅 <Bunnies>", LocalDateTime.of(2027, 4, 5, 18, 0), "K-POP", "올림픽공원 체조경기장", 110000, "g9", "🐰",
                "뉴진스와 함께하는 스페셜 팬미팅 <Bunnies>. 토크, 미니게임, 라이브 무대까지 알찬 구성으로 준비했다."),
            event("John Mayer Asia Tour", LocalDateTime.of(2027, 5, 20, 20, 0), "POP", "블루스퀘어 마스터카드홀", 143000, "g10", "🎸",
                "블루스와 팝을 넘나드는 존 메이어의 기타 라이브. 감각적인 즉흥 연주가 돋보이는 아시아 투어."),
            event("에픽하이 2025 이벤트", LocalDateTime.of(2027, 3, 15, 19, 0), "힙합/R&B", "올림픽홀", 99000, "g11", "🎤",
                "타블로, 미쓰라진, 투컷의 에픽하이가 선사하는 힙합 라이브. 시대를 관통하는 가사와 묵직한 사운드."),
            event("Day6 3rd World Tour", LocalDateTime.of(2027, 4, 12, 19, 0), "K-POP", "YES24 라이브홀", 88000, "g12", "🎸",
                "밴드 사운드의 진수를 보여주는 데이식스의 월드투어. 직접 연주하는 라이브 밴드만의 생생함이 매력."),

            // J-POP
            event("ZUTOMAYO Live Tour 2027", LocalDateTime.of(2027, 8, 14, 19, 0), "J-POP", "올림픽공원 SK핸드볼경기장", 143000, "g1", "🌙",
                "정체를 숨긴 채 독창적인 음악 세계를 펼치는 즛토마요의 라이브 투어. 몽환적인 영상 연출과 개성 있는 사운드가 특징."),
            event("Vaundy Asia Tour 2027", LocalDateTime.of(2027, 9, 5, 19, 0), "J-POP", "YES24 라이브홀", 132000, "g5", "🎧",
                "장르를 자유롭게 넘나드는 신세대 아티스트 바운디의 아시아 투어. 세련된 사운드 프로듀싱을 라이브로 만나본다."),
            event("Ado Special Live in Seoul", LocalDateTime.of(2027, 10, 2, 19, 0), "J-POP", "KSPO DOME", 165000, "g11", "🎭",
                "폭발적인 가창력의 아도가 선사하는 스페셜 라이브. 강렬한 비주얼 연출과 압도적인 보컬이 무대를 채운다."),
            event("Tuki. Asia Tour 2027", LocalDateTime.of(2027, 11, 8, 19, 0), "J-POP", "블루스퀘어 마스터카드홀", 110000, "g6", "✨",
                "화제의 신예 아티스트 츠키의 아시아 투어. 감성적인 어쿠스틱 사운드와 담백한 목소리가 매력."),
            event("Aimyon Live in Seoul", LocalDateTime.of(2027, 7, 18, 18, 0), "J-POP", "올림픽공원 88잔디마당", 121000, "g4", "🎸",
                "일상의 감정을 노래하는 싱어송라이터 아이묭의 서울 라이브. 담담하지만 깊은 울림을 주는 무대."),

            // 내한 (US POP)
            event("Ariana Grande Eternal Sunshine Tour", LocalDateTime.of(2027, 6, 20, 19, 30), "POP", "고척 스카이돔", 187000, "g7", "🎀",
                "팝의 아이콘 아리아나 그란데의 'Eternal Sunshine Tour'. 폭넓은 음역대와 화려한 무대 연출이 어우러지는 공연."),
            event("Charlie Puth Asia Tour 2027", LocalDateTime.of(2027, 8, 29, 19, 0), "POP", "KSPO DOME", 143000, "g3", "🎹",
                "멀티 악기 연주와 감미로운 보컬의 찰리 푸스, 아시아 투어로 한국을 찾는다. 프로듀서 출신다운 섬세한 라이브 편곡이 돋보인다."),
            event("Post Malone Live in Seoul", LocalDateTime.of(2027, 9, 19, 19, 0), "POP", "잠실 올림픽 주경기장", 165000, "g12", "🔥",
                "장르를 넘나드는 힙합 아이콘 포스트 말론의 첫 내한 라이브. 록적인 사운드와 힙합이 뒤섞인 독특한 무대."),
            event("Justin Bieber World Tour 2027", LocalDateTime.of(2027, 10, 24, 19, 0), "POP", "인천 아시아드 주경기장", 176000, "g2", "🎤",
                "팝스타 저스틴 비버의 월드투어가 한국을 찾는다. 커리어를 아우르는 히트곡 무대와 화려한 퍼포먼스로 채워지는 밤.")
        );

        eventRepository.saveAll(events);

        List<Seat> seats = new ArrayList<>(events.size() * ROWS * COLS);
        for (Event savedEvent : events) {
            for (char row = 'A'; row < 'A' + ROWS; row++) {
                for (int col = 1; col <= COLS; col++) {
                    seats.add(new Seat(savedEvent, row + String.valueOf(col), SeatStatus.AVAILABLE));
                }
            }
        }
        seatRepository.saveAll(seats);

        log.info("[dev-seed] 이벤트 {}건, 좌석 {}건 생성 완료", events.size(), seats.size());
    }

    private Event event(String title, LocalDateTime startAt, String category, String venue,
        Integer price, String themeColor, String thumbnailEmoji, String description) {
        return new Event(null, title, startAt, category, venue, price, null, null,
            EventStatus.ACTIVE, themeColor, thumbnailEmoji, description);
    }
}
