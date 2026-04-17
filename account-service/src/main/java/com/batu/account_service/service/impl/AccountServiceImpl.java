package com.batu.account_service.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.batu.account_service.entity.Account;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.exception.ResourceNotFoundException;
import com.batu.account_service.mapper.AccountSyncMapper;
import com.batu.account_service.messaging.AccountsPersistedDomainEvent;
import com.batu.account_service.repository.AccountRepository;
import com.batu.account_service.repository.specs.AccountSpecification;
import com.batu.account_service.service.AccountService;
import com.batu.account_service.util.CursorUtils;
import com.batu.shared.dto.request.AccountsUpsertRequestDto;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountCurrencyTotalDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;

import jakarta.transaction.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

        private final AccountRepository accountRepository;
        private final CursorUtils cursorUtils;
        private final ApplicationEventPublisher eventPublisher;
        private final AccountSyncMapper accountSyncMapper;

        public AccountServiceImpl(AccountRepository accountRepository,
                        CursorUtils cursorUtils,
                        ApplicationEventPublisher eventPublisher,
                        AccountSyncMapper accountSyncMapper) {
                this.accountRepository = accountRepository;
                this.cursorUtils = cursorUtils;
                this.eventPublisher = eventPublisher;
                this.accountSyncMapper = accountSyncMapper;
        }

        @Override
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

                Window<AccountViewDto> accounts = accountRepository.findBy(spec, query -> query
                                .as(AccountViewDto.class)
                                .sortBy(sort)
                                .limit(limit)
                                .scroll(scrollPosition));

                String nextCursor = accounts.hasNext()
                                ? cursorUtils.encode(accounts.positionAt(accounts.size() - 1))
                                : null;

                return new CursorResponse<>(accounts.getContent(), accounts.hasNext(), nextCursor);
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
        public void saveBatch(AccountsUpsertRequestDto request) {
                syncAccounts(request.getAccounts().stream()
                                .map(accountSyncMapper::toEntity)
                                .toList());
        }

        @Override
        public List<UUID> getAccountIdsByConnection(UUID connectionId) {
                return accountRepository.findByConnectionId(connectionId).stream()
                                .map(Account::getAccountId)
                                .toList();
        }

        @Override
        @Transactional
        public void deactivateByConnection(UUID connectionId) {
                List<Account> accounts = accountRepository.findByConnectionId(connectionId).stream()
                                .filter(Account::isActive)
                                .toList();

                if (accounts.isEmpty()) {
                        return;
                }

                for (Account account : accounts) {
                        account.setActive(false);
                }

                accountRepository.saveAll(accounts);
                publishPersistedEvents(accounts);
        }

        private void syncAccounts(List<Account> requestedAccounts) {
                if (requestedAccounts.isEmpty()) {
                        return;
                }

                Map<UUID, Account> existingAccountsById = accountRepository.findAllByAccountIdIn(
                                requestedAccounts.stream()
                                                .map(Account::getAccountId)
                                                .toList())
                                .stream()
                                .collect(Collectors.toMap(Account::getAccountId, account -> account));

                List<Account> accountsToPersist = new ArrayList<>();
                List<Account> changedAccounts = new ArrayList<>();

                for (Account requestedAccount : requestedAccounts) {
                        Account existingAccount = existingAccountsById.get(requestedAccount.getAccountId());

                        if (existingAccount == null) {
                                accountsToPersist.add(requestedAccount);
                                changedAccounts.add(requestedAccount);
                                continue;
                        }

                        validateUserOwnership(existingAccount, requestedAccount.getUserId());

                        if (!applyAccountState(existingAccount, requestedAccount)) {
                                continue;
                        }

                        accountsToPersist.add(existingAccount);
                        changedAccounts.add(existingAccount);
                }

                if (accountsToPersist.isEmpty()) {
                        return;
                }

                accountRepository.saveAll(accountsToPersist);
                publishPersistedEvents(changedAccounts);
        }

        private boolean applyAccountState(Account target, Account source) {
                boolean changed = false;

                changed |= updateIfChanged(target.getConnectionId(), source.getConnectionId(), target::setConnectionId);
                changed |= updateIfChanged(target.getInstitutionName(), source.getInstitutionName(), target::setInstitutionName);
                changed |= updateIfChanged(target.getAccountName(), source.getAccountName(), target::setAccountName);
                changed |= updateIfChanged(target.getAccountType(), source.getAccountType(), target::setAccountType);
                changed |= updateIfChanged(target.getAccountSubtype(), source.getAccountSubtype(), target::setAccountSubtype);
                changed |= updateIfChanged(target.getAccountMask(), source.getAccountMask(), target::setAccountMask);
                changed |= updateIfChanged(target.getCurrentBalance(), source.getCurrentBalance(), target::setCurrentBalance);
                changed |= updateIfChanged(target.getAvailableBalance(), source.getAvailableBalance(), target::setAvailableBalance);
                changed |= updateIfChanged(target.getIsoCurrencyCode(), source.getIsoCurrencyCode(), target::setIsoCurrencyCode);
                changed |= updateIfChanged(target.isActive(), source.isActive(), target::setActive);

                return changed;
        }

        private void publishPersistedEvents(List<Account> accounts) {
                eventPublisher.publishEvent(new AccountsPersistedDomainEvent(
                                accounts.stream()
                                                .map(accountSyncMapper::toPersistedEvent)
                                                .toList()));
        }

        private void validateUserOwnership(Account existingAccount, UUID requestedUserId) {
                if (!existingAccount.getUserId().equals(requestedUserId)) {
                        throw new IllegalStateException("Account ownership mismatch for account " + existingAccount.getAccountId());
                }
        }

        private boolean updateIfChanged(String currentValue, String nextValue, Consumer<String> consumer) {
                if (Objects.equals(currentValue, nextValue)) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(UUID currentValue, UUID nextValue, Consumer<UUID> consumer) {
                if (Objects.equals(currentValue, nextValue)) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(BigDecimal currentValue, BigDecimal nextValue, Consumer<BigDecimal> consumer) {
                if (currentValue == null && nextValue == null) {
                        return false;
                }

                if (currentValue != null && nextValue != null && currentValue.compareTo(nextValue) == 0) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(boolean currentValue, boolean nextValue, Consumer<Boolean> consumer) {
                if (currentValue == nextValue) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }
}
