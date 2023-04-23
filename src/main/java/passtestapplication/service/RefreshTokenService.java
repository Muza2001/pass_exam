package passtestapplication.service;


import passtestapplication.dto.request.RefreshTokenRequest;
import passtestapplication.model.auth.RefreshToken;

public interface RefreshTokenService {

    RefreshToken generateRefreshToken();

    void validationToken(String refreshToken);

    void refreshTokenDelete(RefreshTokenRequest request);

}
