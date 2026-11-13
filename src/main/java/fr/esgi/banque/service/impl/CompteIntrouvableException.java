package fr.esgi.banque.service.impl;

public class CompteIntrouvableException extends RuntimeException {

	public CompteIntrouvableException(String numeroCompte) {
		super("Compte introuvable : " + numeroCompte);
	}
}
