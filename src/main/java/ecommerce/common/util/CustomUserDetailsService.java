package ecommerce.common.util;

import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.common.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + usernameOrEmail));

        return UserPrincipal.create(user);
    }

    /**
     * Load user by UUID identifier.
     * @param id The user's unique identifier (UUID)
     * @return UserDetails for the user
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return UserPrincipal.create(user);
    }

    /**
     * Load user by Long identifier (legacy support).
     * @param id The user's unique identifier (Long)
     * @return UserDetails for the user
     * @deprecated Use {@link #loadUserById(UUID)} instead
     */
    @Deprecated(since = "2.0")
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        // For backward compatibility - convert Long to UUID if needed
        // This requires the repository to handle the conversion
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", id));
        return loadUserById(uuid);
    }
}
