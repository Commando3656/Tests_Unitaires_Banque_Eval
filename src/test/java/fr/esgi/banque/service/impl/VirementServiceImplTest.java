package fr.esgi.banque.service.impl;

import fr.esgi.banque.business.Compte;
import fr.esgi.banque.business.SoldeInsuffisantException;
import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VirementServiceImplTest {

	private CompteRepository repository;
	private NotificationService notifications;
	private Compte source;
	private Compte destination;

	@BeforeEach
	void setUp() {
		repository = mock(CompteRepository.class);
		notifications = mock(NotificationService.class);
		source = new Compte("SRC", "Alice", new BigDecimal("20000"), new BigDecimal("0"));
		destination = new Compte("DST", "Bob", new BigDecimal("100"), new BigDecimal("0"));
		when(repository.trouver("SRC")).thenReturn(Optional.of(source));
		when(repository.trouver("DST")).thenReturn(Optional.of(destination));
	}

	private VirementServiceImpl service(String date) {
		Clock clock = Clock.fixed(Instant.parse(date + "T12:00:00Z"), ZoneOffset.UTC);
		return new VirementServiceImpl(repository, notifications, clock);
	}

	@ParameterizedTest
	@CsvSource({",DST", "0,DST", "-0.01,DST"})
	void shouldRejectNullZeroOrNegativeAmount(String montant, String destinationNumero) {
		VirementServiceImpl service = service("2026-11-13");
		BigDecimal montantValue = montant == null ? null : new BigDecimal(montant);

		assertThatThrownBy(() -> service.virer("SRC", destinationNumero, montantValue))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(repository, notifications);
	}

	@Test
	void shouldRejectTransferToSameAccount() {
		assertThatThrownBy(() -> service("2026-11-13").virer("SRC", "SRC", BigDecimal.ONE))
				.isInstanceOf(IllegalArgumentException.class);
		verifyNoInteractions(repository, notifications);
	}

	@Test
	void shouldThrowWhenSourceAccountDoesNotExist() {
		when(repository.trouver("MISSING")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service("2026-11-13").virer("MISSING", "DST", BigDecimal.ONE))
				.isInstanceOf(CompteIntrouvableException.class);
		verify(repository, never()).sauvegarder(any());
	}

	@Test
	void shouldThrowWhenDestinationAccountDoesNotExist() {
		when(repository.trouver("MISSING")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service("2026-11-13").virer("SRC", "MISSING", BigDecimal.ONE))
				.isInstanceOf(CompteIntrouvableException.class);
		verify(repository, never()).sauvegarder(any());
	}

	@Test
	void shouldDebitCreditSaveBothAndReturnTransferWithCurrentDate() {
		Virement virement = service("2026-11-13").virer("SRC", "DST", new BigDecimal("250"));

		assertThat(source.getSolde()).isEqualByComparingTo("19750");
		assertThat(destination.getSolde()).isEqualByComparingTo("350");
		assertThat(virement.numeroSource()).isEqualTo("SRC");
		assertThat(virement.numeroDestination()).isEqualTo("DST");
		assertThat(virement.montant()).isEqualByComparingTo("250");
		assertThat(virement.date()).isEqualTo(LocalDate.of(2026, 11, 13));
		verify(repository).sauvegarder(source);
		verify(repository).sauvegarder(destination);
	}

	@Test
	void shouldNotSaveAnyAccountWhenSourceHasInsufficientFunds() {
		source = new Compte("SRC", "Alice", new BigDecimal("10"), BigDecimal.ZERO);
		when(repository.trouver("SRC")).thenReturn(Optional.of(source));

		assertThatThrownBy(() -> service("2026-11-13").virer("SRC", "DST", new BigDecimal("11")))
				.isInstanceOf(SoldeInsuffisantException.class);
		verify(repository, never()).sauvegarder(any());
		assertThat(destination.getSolde()).isEqualByComparingTo("100");
	}

	@ParameterizedTest
	@CsvSource({"10000,false", "10000.01,true"})
	void shouldAlertOnlyForTransfersAboveTenThousand(String montant, boolean alerteAttendue) {
		service("2026-11-13").virer("SRC", "DST", new BigDecimal(montant));

		if (alerteAttendue) {
			verify(notifications).alerter(eq("Alice"), anyString());
		} else {
			verify(notifications, never()).alerter(anyString(), anyString());
		}
	}

	@Test
	void shouldRejectTransferOnSundayWithoutSavingAccounts() {
		assertThatThrownBy(() -> service("2026-11-15").virer("SRC", "DST", BigDecimal.ONE))
				.isInstanceOf(VirementRefuseException.class);
		verify(repository, never()).sauvegarder(any());
	}
}
