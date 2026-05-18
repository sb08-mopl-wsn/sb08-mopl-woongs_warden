package com.mopl.mopl.infrastructure.external.constants;

import java.util.List;

public class ExternalApiConstants
{
    /* sports */
    public static final String EVENTS_DAY_PATH = "/eventsday.php?d={date}&l={leagueId}";

    public static final int PREMIER_LEAGUE_ID = 4328;
    public static final int LA_LIGA_ID = 4335;
    public static final int SERIE_A = 4332;
    public static final int LEGUE_UN = 4334;
    public static final int BUNDES_LIGA = 4331;
    public static final int K_LEAGUE = 4689;
    public static final int LCK = 4529;

    public static final List<Integer> LEAGUE_IDS = List.of(
            PREMIER_LEAGUE_ID, LA_LIGA_ID, SERIE_A, LEGUE_UN, BUNDES_LIGA, K_LEAGUE, LCK
    );

    /* tmdb */
    public static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    public static final String MOVIE_GENRE_LIST = "/genre/movie/list?language=ko-KR";
    public static final String TV_GENRE_LIST = "/genre/tv/list?language=ko-KR";

    public static final String DISCOVER_MOVIE = "/discover/movie?language=ko-KR&page={page}&sort_by=popularity.desc";
    public static final String DISCOVER_TV = "/discover/tv?language=ko-KR&page={page}&sort_by=popularity.desc";

    private ExternalApiConstants() {}
}
