package com.batu.account_service.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import io.micrometer.observation.annotation.Observed;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.account_service.entity.Account;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.exception.ResourceNotFoundException;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.account_service.service.AccountService;
import com.batu.account_service.service.input.RecordAccountInput;
import com.batu.account_service.util.CursorUtils;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.response.AccountCurrencyTotalDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;

import jakarta.transaction.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

        private final AccountRepository accountRepository;
        private final CursorUtils cursorUtils;

        public AccountServiceImpl(AccountRepository accountRepository,
                        CursorUtils cursorUtils) {
                this.accountRepository = accountRepository;
                this.cursorUtils = cursorUtils;
        }

        @Override
        @Observed(name = "account.list", contextualName = "account list accounts")
        public CursorResponse<AccountViewDto> getAccountsViewPaginated(
                        Jwt principal,
                        String accountName,
                        String institutionName,
                        String accountType,
                        String accountSubtype,
                        String cursor,
                        int limit,
                        AccountSortField sortBy,
                        Sort.Direction direction) {
                UUID userId = UUID.fromString(principal.getSubject());

                Sort sort = sortBy == null
                                ? Sort.by(direction, "createdAt").and(Sort.by("accountId"))
                                : Sort.by(direction, sortBy.getFieldName()).and(Sort.by("accountId"));

                ScrollPosition scrollPosition = cursor != null
                                ? cursorUtils.decode(cursor)
                                : ScrollPosition.keyset();

                var spec = AccountSpecification.filter(userId, accountName, institutionName, accountType, accountSubtype);

                Window<Account> accounts = accountRepository.findBy(spec, query -> query
                                .sortBy(sort)
                                .limit(limit)
                                .scroll(scrollPosition));

                String nextCursor = accounts.hasNext()
                                ? cursorUtils.encode(accounts.positionAt(accounts.size() - 1))
                                : null;

                List<AccountViewDto> content = accounts.getContent().stream()
                                .map(account -> new AccountViewDto(
                                                account.getAccountId(),
                                                account.getInstitutionName(),
                                                account.getAccountName(),
                                                account.getCurrentBalance(),
                                                account.getAvailableBalance(),
                                                account.getIsoCurrencyCode(),
                                                account.getAccountType(),
                                                account.getAccountSubtype(),
                                                account.getAccountMask(),
                                                account.getCreatedAt(),
                                                account.getUpdatedAt()))
                                .toList();

                return new CursorResponse<>(content, accounts.hasNext(), nextCursor);
        }

        @Override
        public AccountResponseDto getAccount(UUID accountId, Jwt principal) {
                UUID userId = UUID.fromString(principal.getSubject());
                return accountRepository.findByAccountIdAndUserIdAndIsActiveTrue(accountId, userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "This account is not exists, or access restricted."));
        }

        @Override
        public AccountSummaryResponseDto getAccountSummary(Jwt principal) {
                UUID userId = UUID.fromString(principal.getSubject());
                List<AccountCurrencyTotalDto> totalsByCurrency = accountRepository.summarizeActiveBalancesByCurrency(userId)
                                .stream()
                                .map(total -> new AccountCurrencyTotalDto(
                                                total.getIsoCurrencyCode(),
                                                total.getCurrentBalanceTotal(),
                                                total.getAvailableBalanceTotal()))
                                .toList();

                return new AccountSummaryResponseDto(
                                userId,
                                accountRepository.countByUserIdAndIsActiveTrue(userId),
                                totalsByCurrency);
        }

        @Override
        public List<AccountNameResponseDto> getAccountsByGivenIds(AccountNameRequestDto request) {
                return accountRepository.findByAccountIdIn(request.getAccountIds());
        }

        @Override
        @Transactional
        public Optional<Account> recordAccount(RecordAccountInput input) {
                Account account = accountRepository.findById(input.accountId()).orElse(null);

                if (account != null && input.version() <= account.getSyncVersion()) {
                        return Optional.empty();
                }

                if (account == null) {
                        account = new Account(
                                        input.accountId(),
                                        input.userId(),
                                        input.connectionId(),
                                        input.institutionName(),
                                        input.accountName(),
                                        input.accountType(),
                                        input.accountSubtype(),
                                        input.accountMask(),
                                        input.currentBalance(),
                                        input.availableBalance(),
                                        input.isoCurrencyCode(),
                                        true,
                                        input.version());
                } else {
                        account.setUserId(input.userId());
                        account.setConnectionId(input.connectionId());
                        account.setInstitutionName(input.institutionName());
                        account.setAccountName(input.accountName());
                        account.setAccountType(input.accountType());
                        account.setAccountSubtype(input.accountSubtype());
                        account.setAccountMask(input.accountMask());
                        account.setCurrentBalance(input.currentBalance());
                        account.setAvailableBalance(input.availableBalance());
                        account.setIsoCurrencyCode(input.isoCurrencyCode());
                        account.setActive(true);
                        account.setSyncVersion(input.version());
                }

                return Optional.of(accountRepository.save(account));
        }

        @Override
        @Transactional
        public List<Account> deactivateByConnection(UUID connectionId, long version) {
                List<Account> accounts = accountRepository.findByConnectionIdAndIsActiveTrue(connectionId)
                                .stream()
                                .filter(account -> version > account.getSyncVersion())
                                .toList();

                for (Account account : accounts) {
                        account.setActive(false);
                        account.setSyncVersion(version);
                }

                return accountRepository.saveAll(accounts);
        }

}
