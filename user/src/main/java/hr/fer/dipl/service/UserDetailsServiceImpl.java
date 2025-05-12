package hr.fer.dipl.service;



import hr.fer.dipl.db.model.User;
import hr.fer.dipl.db.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final User owner = this.repository.findByUsername(username);
        if (owner == null) {
            throw new UsernameNotFoundException("Unknown user " + username);
        }
        return owner;
    }

}