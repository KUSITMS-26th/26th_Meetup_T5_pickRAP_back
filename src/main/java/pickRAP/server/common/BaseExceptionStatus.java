package pickRAP.server.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BaseExceptionStatus {

    // 성공
    SUCCESS(HttpStatus.OK, 1000, "요청 성공"),
    // 인증
    UN_AUTHORIZED(HttpStatus.UNAUTHORIZED, 2000, "토큰 검증 실패"),
    SC_FORBIDDEN(HttpStatus.FORBIDDEN, 2001, "권한 없음"),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, 2002, "이메일 형식을 확인해주세요"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, 2003, "비밀번호 형식을 확인해주세요"),
    EXIST_ACCOUNT(HttpStatus.BAD_REQUEST, 2004, "이미 존재하는 회원입니다"),
    FAIL_LOGIN(HttpStatus.BAD_REQUEST, 2005, "로그인 실패"),
    INVALID_EMAIL_CODE(HttpStatus.BAD_REQUEST, 2006, "인증코드 검증실패"),

    // 회원


    // 스크랩
    NOT_SUPPORT_FILE(HttpStatus.BAD_REQUEST, 4001, "지원하지 않는 파일 형식입니다"),
    FILE_UPLOAD_FAIL(HttpStatus.BAD_REQUEST, 4002, "파일 업로드 실패"),
    FILE_DOWNLOAD_FAIL(HttpStatus.BAD_REQUEST, 4003, "파일 다운로드 실패");

    // 메거진


    // 분석&추천

    // 커뮤니티


    private HttpStatus httpStatus;
    private final int code;
    private final String message;

    private BaseExceptionStatus(HttpStatus httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
