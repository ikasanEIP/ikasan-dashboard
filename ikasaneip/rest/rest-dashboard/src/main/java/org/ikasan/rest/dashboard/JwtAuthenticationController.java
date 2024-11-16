package org.ikasan.rest.dashboard;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.ikasan.rest.dashboard.model.dto.JwtRequest;
import org.ikasan.rest.dashboard.model.dto.JwtResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@CrossOrigin
public class JwtAuthenticationController
{
    private AuthenticationManager authenticationManager;

    private JwtTokenUtil jwtTokenUtil;

    private UserDetailsService userDetailsService;

    public JwtAuthenticationController(AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil,
                                       UserDetailsService userDetailsService)
    {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @RequestMapping (value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest
        , HttpServletRequest request) throws Exception
    {
        try {
            // No need to re-authenticate if the token in the header is already valid!
            if (this.determineIfTokenAlreadyValid(request, authenticationRequest)) {
                return ResponseEntity.ok(new JwtResponse(this.getJwtTokenFromHeader(request)));
            }
        } catch (ExpiredJwtException e) {
            // the token has expired so will let the authentication happen as normal.
        }

        authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authenticationRequest.getUsername());
        final String token = jwtTokenUtil.generateToken(userDetails);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @RequestMapping (value = "/isAuthenticated", method = RequestMethod.GET)
    public ResponseEntity<?> isAuthenticated(@RequestBody JwtRequest authenticationRequest
        , HttpServletRequest request) throws Exception
    {
        try {
            if (this.determineIfTokenAlreadyValid(request, authenticationRequest)) {
                ResponseEntity.ok(new JwtResponse(this.getJwtTokenFromHeader(request)));
                ResponseEntity.ok();
            }
        } catch (ExpiredJwtException e) {
            // the token has expired so will let the authentication happen as normal.
        }

        return ResponseEntity.badRequest().build();
    }

    /**
     * Authenticates the user with the provided username and password.
     *
     * @param username the username of the user to authenticate
     * @param password the password of the user to authenticate
     * @throws Exception if the user is disabled or the credentials are invalid
     */
    private void authenticate(String username, String password) throws Exception
    {
        try
        {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        }
        catch (DisabledException e)
        {
            throw new Exception("USER_DISABLED", e);
        }
        catch (BadCredentialsException e)
        {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }

    /**
     * Retrieves the JWT token from the Authorization header in the provided HttpServletRequest.
     *
     * @param httpServletRequest the HTTP request containing the JWT token in the Authorization header
     * @return the JWT token extracted from the Authorization header, or null if not found or invalid format
     */
    private String getJwtTokenFromHeader(HttpServletRequest httpServletRequest) {
        final String requestTokenHeader = httpServletRequest.getHeader("Authorization");
        String jwtToken = null;
        // JWT Token is in the form "Bearer token". Remove Bearer word and get
        // only the Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            return jwtToken;
        }

        return null;
    }
    /**
     * Determines if the provided JWT token is already valid.
     *
     * @param httpServletRequest the HTTP request containing the JWT token
     * @return true if the token is valid and not expired; false otherwise
     */
    private boolean determineIfTokenAlreadyValid(HttpServletRequest httpServletRequest,
                                                 JwtRequest authenticationRequest) {
        String jwtToken = this.getJwtTokenFromHeader(httpServletRequest);

        if(jwtToken == null) {
            return false;
        }
        else {
            String username = jwtTokenUtil.getUsernameFromToken(jwtToken);
            return (this.jwtTokenUtil.getExpirationDateFromToken(jwtToken).after(new Date()) &&
                username.equals(authenticationRequest.getUsername()));
        }
    }
}