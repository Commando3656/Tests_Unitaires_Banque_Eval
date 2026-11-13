package fr.esgi.banque.service.impl;

import fr.esgi.banque.business.Compte;
import fr.esgi.banque.service.CompteRepository;
import fr.esgi.banque.service.NotificationService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

public class VirementServiceImpl {

	private static final BigDecimal SEUIL_ALERTE = new BigDecimal("10000");

	private final CompteRepository repository;
	private final NotificationService notifications;
	private final Clock horloge;

	public VirementServiceImpl(CompteRepository repository, NotificationService notifications, Clock horloge) {
		this.repository = Objects.requireNonNull(repository);
		this.notifications = Objects.requireNonNull(notifications);
		this.horloge = Objects.requireNonNull(horloge);
	}

	public Virement virer(String numeroSource, String numeroDestination, BigDecimal montant) {
		if (montant == null || montant.signum() <= 0) {
			throw new IllegalArgumentException("Le montant du virement doit être strictement positif");
		}
		if (Objects.equals(numeroSource, numeroDestination)) {
			throw new IllegalArgumentException("La source et la destination doivent être différentes");
		}

		Compte source = repository.trouver(numeroSource)
				.orElseThrow(() -> new CompteIntrouvableException(numeroSource));
		Compte destination = repository.trouver(numeroDestination)
				.orElseThrow(() -> new CompteIntrouvableException(numeroDestination));

		LocalDate date = LocalDate.now(horloge);
		if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
			throw new VirementRefuseException("Les virements ne sont pas acceptés le dimanche");
		}

		source.debiter(montant);
		destination.crediter(montant);
		repository.sauvegarder(source);
		repository.sauvegarder(destination);

		if (montant.compareTo(SEUIL_ALERTE) > 0) {
			notifications.alerter(source.getTitulaire(), "Virement important de " + montant + " €");
		}

		return new Virement(numeroSource, numeroDestination, montant, date);
	}
}
