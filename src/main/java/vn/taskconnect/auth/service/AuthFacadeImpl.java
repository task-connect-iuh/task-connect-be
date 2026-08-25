package vn.taskconnect.auth.service;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import vn.taskconnect.auth.api.AuthFacade;
import vn.taskconnect.auth.api.dto.AccountSummary;
import vn.taskconnect.auth.entity.AuthAccountRole;
import vn.taskconnect.auth.repository.AuthAccountRepository;
import vn.taskconnect.auth.repository.AuthAccountRoleRepository;

@Service
class AuthFacadeImpl implements AuthFacade {

    private final AuthAccountRepository accountRepository;
    private final AuthAccountRoleRepository accountRoleRepository;

    AuthFacadeImpl(AuthAccountRepository accountRepository, AuthAccountRoleRepository accountRoleRepository) {
        this.accountRepository = accountRepository;
        this.accountRoleRepository = accountRoleRepository;
    }

    @Override
    public Optional<AccountSummary> findAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .map(account -> new AccountSummary(
                        account.getId(),
                        account.getEmail(),
                        account.getPhone(),
                        account.getStatus(),
                        accountRoleRepository.findByAccountId(accountId).stream()
                                .map(AuthAccountRole::getRole)
                                .collect(Collectors.toCollection(LinkedHashSet::new))));
    }
}
