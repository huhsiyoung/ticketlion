package site.ticketlion.web.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String displayName;
    private String username;
    private String password;
    private String passwordConfirm;
}