import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import {
  ExchangeLoginResponse,
  ExchangeMessage,
  ExchangeRequest,
  ExternalUser,
} from '../models/exchange.model';

/**
 * Service Angular pour le portail externe Exchange.
 *
 * <p>Gère l'authentification des utilisateurs externes ainsi que la création,
 * le suivi et la messagerie des demandes Exchange.
 *
 * <p>Le token Exchange est distinct du JWT interne et est stocké dans
 * localStorage sous la clé {@code exchange_access_token}.
 */
@Injectable({ providedIn: 'root' })
export class ExchangeService {

  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/exchange';

  /** Clé localStorage pour le token Exchange externe. */
  static readonly TOKEN_KEY = 'exchange_access_token';

  // ─────────────────────── Helpers ───────────────────────

  /** Construit les headers d'authentification Exchange. */
  private authHeaders(): HttpHeaders {
    const token = localStorage.getItem(ExchangeService.TOKEN_KEY);
    return token
      ? new HttpHeaders({ Authorization: `Bearer ${token}` })
      : new HttpHeaders();
  }

  // ─────────────────────── Auth externe ───────────────────────

  /**
   * Inscrit un nouvel utilisateur externe.
   *
   * @param req Données d'inscription (email, password, firstName, lastName, organization, role)
   * @returns L'utilisateur externe créé
   */
  register(req: Record<string, unknown>): Observable<ExternalUser> {
    return this.http.post<ExternalUser>(`${this.base}/auth/register`, req);
  }

  /**
   * Authentifie un utilisateur externe et stocke le token dans localStorage.
   *
   * @param email Adresse email
   * @param password Mot de passe
   * @returns La réponse de login contenant le token et l'utilisateur
   */
  login(email: string, password: string): Observable<ExchangeLoginResponse> {
    return this.http.post<ExchangeLoginResponse>(`${this.base}/auth/login`, { email, password }).pipe(
      tap(res => localStorage.setItem(ExchangeService.TOKEN_KEY, res.accessToken))
    );
  }

  /**
   * Vérifie l'adresse email via le token reçu par email.
   *
   * @param token Token de vérification
   * @returns L'utilisateur mis à jour
   */
  verifyEmail(token: string): Observable<ExternalUser> {
    return this.http.get<ExternalUser>(`${this.base}/auth/verify-email`, { params: { token } });
  }

  // ─────────────────────── Demandes ───────────────────────

  /**
   * Crée une nouvelle demande Exchange.
   *
   * @param req Données partielles de la demande
   * @returns La demande créée
   */
  createRequest(req: Partial<ExchangeRequest>): Observable<ExchangeRequest> {
    return this.http.post<ExchangeRequest>(`${this.base}/requests`, req, {
      headers: this.authHeaders(),
    });
  }

  /**
   * Récupère toutes les demandes de l'utilisateur connecté.
   *
   * @returns Liste des demandes
   */
  getMyRequests(): Observable<ExchangeRequest[]> {
    return this.http.get<ExchangeRequest[]>(`${this.base}/requests/my`, {
      headers: this.authHeaders(),
    });
  }

  /**
   * Récupère le détail d'une demande par son identifiant.
   *
   * @param id Identifiant de la demande
   * @returns La demande correspondante
   */
  getRequestById(id: string): Observable<ExchangeRequest> {
    return this.http.get<ExchangeRequest>(`${this.base}/requests/${id}`, {
      headers: this.authHeaders(),
    });
  }

  /**
   * Soumet officiellement une demande (passage de DRAFT → SUBMITTED).
   *
   * @param id Identifiant de la demande
   * @returns La demande mise à jour
   */
  submitRequest(id: string): Observable<ExchangeRequest> {
    return this.http.post<ExchangeRequest>(`${this.base}/requests/${id}/submit`, {}, {
      headers: this.authHeaders(),
    });
  }

  // ─────────────────────── Messagerie ───────────────────────

  /**
   * Récupère les messages d'une demande.
   *
   * @param requestId Identifiant de la demande
   * @returns Liste des messages ordonnés par date
   */
  getMessages(requestId: string): Observable<ExchangeMessage[]> {
    return this.http.get<ExchangeMessage[]>(`${this.base}/requests/${requestId}/messages`, {
      headers: this.authHeaders(),
    });
  }

  /**
   * Envoie un message dans le fil de discussion d'une demande.
   *
   * @param requestId Identifiant de la demande
   * @param content Contenu du message
   * @returns Le message créé
   */
  sendMessage(requestId: string, content: string): Observable<ExchangeMessage> {
    return this.http.post<ExchangeMessage>(
      `${this.base}/requests/${requestId}/messages`,
      { content },
      { headers: this.authHeaders() }
    );
  }
}
