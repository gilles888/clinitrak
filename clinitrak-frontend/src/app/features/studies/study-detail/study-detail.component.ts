import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { StudyService } from '../../../core/services/study.service';
import { hasAnyRole } from '../../../core/store/auth.store';
import { SystemRole } from '../../../core/models/user.model';
import {
  ContactRequest,
  ContactResponse,
  CONTACT_TYPE_OPTIONS,
  ContactType,
  PatientRequest,
  PatientResponse,
  PATIENT_STATUS_OPTIONS,
  PatientStatus,
  StatusUpdateRequest,
  StudyResponse,
  StudyStatus,
  STUDY_STATUS_OPTIONS,
  StudyStatusHistory,
  SubmissionRequest,
  SubmissionResponse,
  SUBMISSION_TYPE_OPTIONS,
  SubmissionType,
} from '../../../core/models/study.model';
import { StudyStatusBadgeComponent } from '../../../shared/components/study-status-badge/study-status-badge.component';

/** Libellés des sévérités de soumission pour les tags PrimeNG. */
const SUBMISSION_STATUS_SEVERITY: Record<string, 'success' | 'info' | 'warning' | 'danger' | 'secondary'> = {
  PENDING:      'warning',
  SUBMITTED:    'info',
  ACKNOWLEDGED: 'info',
  APPROVED:     'success',
  REJECTED:     'danger',
  WITHDRAWN:    'secondary',
};

/** Libellés français des statuts de soumission. */
const SUBMISSION_STATUS_LABELS: Record<string, string> = {
  PENDING:      'En attente',
  SUBMITTED:    'Soumis',
  ACKNOWLEDGED: 'Accusé réception',
  APPROVED:     'Approuvé',
  REJECTED:     'Rejeté',
  WITHDRAWN:    'Retiré',
};

/** Libellés français des statuts patient. */
const PATIENT_STATUS_LABELS: Record<string, string> = {
  SCREENED:      'Screené',
  ENROLLED:      'Inscrit',
  ONGOING:       'En cours',
  COMPLETED:     'Terminé',
  WITHDRAWN:     'Retiré',
  SCREEN_FAILED: 'Échec screening',
};

/**
 * Composant de détail d'une étude clinique.
 *
 * <p>Affiche toutes les informations d'une étude dans 6 onglets :
 * Général, Contacts, Soumissions, Patients, Historique, Documents.
 * Les données sont chargées en parallèle au démarrage du composant.
 *
 * <p>Les dialogues de saisie (statut, contact, soumission, patient) sont
 * contrôlés via des signals booléens.
 */
@Component({
  selector: 'app-study-detail',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    ButtonModule,
    CardModule,
    CheckboxModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    InputTextareaModule,
    ProgressBarModule,
    TableModule,
    TabViewModule,
    TagModule,
    TimelineModule,
    ToastModule,
    StudyStatusBadgeComponent,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    @if (isLoading()) {
      <div class="tw-flex tw-justify-center tw-items-center tw-min-h-64">
        <i class="pi pi-spin pi-spinner tw-text-5xl tw-text-blue-500"></i>
      </div>
    }

    @if (!isLoading() && study()) {
      <div class="tw-space-y-4">

        <!-- En-tête -->
        <div class="tw-flex tw-items-start tw-justify-between tw-gap-4">
          <div>
            <div class="tw-flex tw-items-center tw-gap-3 tw-mb-1">
              <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">
                {{ study()!.acronym ?? study()!.studyNumber }}
              </h2>
              <ct-study-status-badge [status]="study()!.currentStatus" />
            </div>
            <p class="tw-text-gray-600 tw-text-sm">{{ study()!.title }}</p>
            <p class="tw-text-xs tw-text-gray-400 tw-font-mono tw-mt-1">{{ study()!.studyNumber }}</p>
          </div>
          <div class="tw-flex tw-gap-2 tw-flex-shrink-0">
            <p-button
              label="Retour"
              icon="pi pi-arrow-left"
              severity="secondary"
              (onClick)="goBack()"
            />
            @if (canEdit()) {
              <p-button
                label="Modifier"
                icon="pi pi-pencil"
                severity="info"
                (onClick)="goToEdit()"
              />
            }
          </div>
        </div>

        <!-- Onglets -->
        <p-tabView [(activeIndex)]="activeTabIndex">

          <!-- ─── Onglet 1 : Général ─── -->
          <p-tabPanel header="Général">
            <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-2 tw-gap-6 tw-pt-4">

              <!-- Numéros réglementaires -->
              <p-card header="Numéros réglementaires" styleClass="tw-border tw-border-gray-100">
                <div class="tw-space-y-3">
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">N° Étude</span>
                    <span class="tw-font-mono tw-text-sm tw-font-medium">{{ study()!.studyNumber }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">N° Éthique</span>
                    <span class="tw-text-sm">{{ study()!.ethicsNumber ?? '—' }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">EudraCT</span>
                    <span class="tw-text-sm">{{ study()!.eudractNumber ?? '—' }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">CTIS</span>
                    <span class="tw-text-sm">{{ study()!.ctisNumber ?? '—' }}</span>
                  </div>
                </div>
              </p-card>

              <!-- Classification -->
              <p-card header="Classification" styleClass="tw-border tw-border-gray-100">
                <div class="tw-space-y-3">
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Type</span>
                    <span class="tw-text-sm tw-font-medium">{{ study()!.studyTypeLabel }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Phase</span>
                    <span class="tw-text-sm">{{ study()!.phaseLabel }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Aire thérapeutique</span>
                    <span class="tw-text-sm">{{ study()!.therapeuticArea ?? '—' }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Sponsor CUSL</span>
                    @if (study()!.isSponsorCusl) {
                      <i class="pi pi-check-circle tw-text-green-600"></i>
                    } @else {
                      <i class="pi pi-times-circle tw-text-gray-400"></i>
                    }
                  </div>
                </div>
              </p-card>

              <!-- Sponsor & Investigateur -->
              <p-card header="Sponsor & Investigateur" styleClass="tw-border tw-border-gray-100">
                <div class="tw-space-y-3">
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Type sponsor</span>
                    <span class="tw-text-sm">{{ study()!.sponsorTypeLabel }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Sponsor</span>
                    <span class="tw-text-sm tw-font-medium">{{ study()!.sponsor }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Investigateur principal</span>
                    <span class="tw-text-sm tw-font-medium">{{ study()!.principalInvestigator }}</span>
                  </div>
                </div>
              </p-card>

              <!-- Dates -->
              <p-card header="Dates clés" styleClass="tw-border tw-border-gray-100">
                <div class="tw-space-y-3">
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Date de début</span>
                    <span class="tw-text-sm">{{ study()!.startDate ? (study()!.startDate | date:'dd/MM/yyyy') : '—' }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Date de fin</span>
                    <span class="tw-text-sm">{{ study()!.endDate ? (study()!.endDate | date:'dd/MM/yyyy') : '—' }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-sm tw-text-gray-500">Date d'approbation</span>
                    <span class="tw-text-sm">{{ study()!.approvalDate ? (study()!.approvalDate | date:'dd/MM/yyyy') : '—' }}</span>
                  </div>
                </div>
              </p-card>

              <!-- Inclusion patients -->
              <p-card header="Inclusion patients" styleClass="tw-border tw-border-gray-100 lg:tw-col-span-2">
                <div class="tw-space-y-3">
                  <div class="tw-flex tw-justify-between tw-items-center">
                    <span class="tw-text-sm tw-text-gray-500">Progression</span>
                    <span class="tw-text-sm tw-font-medium">
                      {{ study()!.currentEnrollment }}
                      @if (study()!.targetEnrollment) {
                        / {{ study()!.targetEnrollment }} patients
                      }
                    </span>
                  </div>
                  @if (study()!.targetEnrollment && study()!.targetEnrollment! > 0) {
                    <p-progressBar
                      [value]="enrollmentPercent()"
                      [showValue]="true"
                      styleClass="tw-h-4"
                    />
                  }
                </div>
              </p-card>

              <!-- Description -->
              @if (study()!.description) {
                <p-card header="Description" styleClass="tw-border tw-border-gray-100 lg:tw-col-span-2">
                  <p class="tw-text-sm tw-text-gray-700 tw-whitespace-pre-wrap">{{ study()!.description }}</p>
                </p-card>
              }

              <!-- Statut et transition -->
              <p-card header="Statut de l'étude" styleClass="tw-border tw-border-gray-100 lg:tw-col-span-2">
                <div class="tw-flex tw-items-center tw-gap-4">
                  <ct-study-status-badge [status]="study()!.currentStatus" />
                  <span class="tw-text-sm tw-text-gray-500">{{ study()!.currentStatusLabel }}</span>
                  @if (canEdit()) {
                    <p-button
                      label="Modifier le statut"
                      icon="pi pi-exchange"
                      severity="warning"
                      size="small"
                      (onClick)="showStatusDialog.set(true)"
                    />
                  }
                </div>
              </p-card>

            </div>
          </p-tabPanel>

          <!-- ─── Onglet 2 : Contacts ─── -->
          <p-tabPanel header="Contacts">
            <div class="tw-pt-4 tw-space-y-4">
              @if (canEdit()) {
                <div class="tw-flex tw-justify-end">
                  <p-button
                    label="Ajouter un contact"
                    icon="pi pi-user-plus"
                    (onClick)="showContactDialog.set(true)"
                  />
                </div>
              }

              <p-table [value]="contacts()" styleClass="tw-border tw-border-gray-200 tw-rounded-lg">
                <ng-template pTemplate="header">
                  <tr>
                    <th>Type</th>
                    <th>Prénom</th>
                    <th>Nom</th>
                    <th>Email</th>
                    <th>Téléphone</th>
                    <th>Organisation</th>
                    <th class="tw-text-center">Principal</th>
                    @if (canEdit()) {
                      <th class="tw-text-center">Actions</th>
                    }
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-contact>
                  <tr>
                    <td><p-tag [value]="contact.contactTypeLabel" severity="info" /></td>
                    <td>{{ contact.firstName }}</td>
                    <td class="tw-font-medium">{{ contact.lastName }}</td>
                    <td>
                      @if (contact.email) {
                        <a [href]="'mailto:' + contact.email" class="tw-text-blue-600 hover:tw-underline">
                          {{ contact.email }}
                        </a>
                      } @else { — }
                    </td>
                    <td>{{ contact.phone ?? '—' }}</td>
                    <td>{{ contact.organization ?? '—' }}</td>
                    <td class="tw-text-center">
                      @if (contact.isPrimary) {
                        <i class="pi pi-star-fill tw-text-yellow-500"></i>
                      }
                    </td>
                    @if (canEdit()) {
                      <td class="tw-text-center">
                        <p-button
                          icon="pi pi-trash"
                          severity="danger"
                          size="small"
                          [rounded]="true"
                          [text]="true"
                          (onClick)="removeContact(contact.id)"
                        />
                      </td>
                    }
                  </tr>
                </ng-template>
                <ng-template pTemplate="emptymessage">
                  <tr>
                    <td [colSpan]="canEdit() ? 8 : 7" class="tw-text-center tw-py-8 tw-text-gray-500">
                      Aucun contact enregistré.
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            </div>
          </p-tabPanel>

          <!-- ─── Onglet 3 : Soumissions ─── -->
          <p-tabPanel header="Soumissions">
            <div class="tw-pt-4 tw-space-y-4">
              @if (canEdit()) {
                <div class="tw-flex tw-justify-end">
                  <p-button
                    label="Ajouter une soumission"
                    icon="pi pi-send"
                    (onClick)="showSubmissionDialog.set(true)"
                  />
                </div>
              }

              <p-table
                [value]="submissions()"
                [paginator]="true"
                [rows]="10"
                styleClass="tw-border tw-border-gray-200 tw-rounded-lg"
              >
                <ng-template pTemplate="header">
                  <tr>
                    <th>Type</th>
                    <th>Date soumission</th>
                    <th>Date limite</th>
                    <th>Statut</th>
                    <th>Soumis par</th>
                    <th>Référence</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-sub>
                  <tr>
                    <td class="tw-text-sm">{{ sub.submissionType }}</td>
                    <td class="tw-text-sm">{{ sub.submissionDate | date:'dd/MM/yyyy' }}</td>
                    <td class="tw-text-sm">{{ sub.dueDate ? (sub.dueDate | date:'dd/MM/yyyy') : '—' }}</td>
                    <td>
                      <p-tag
                        [value]="submissionStatusLabel(sub.status)"
                        [severity]="submissionStatusSeverity(sub.status)"
                      />
                    </td>
                    <td class="tw-text-sm">{{ sub.submittedBy ?? '—' }}</td>
                    <td class="tw-text-sm tw-font-mono">{{ sub.referenceNumber ?? '—' }}</td>
                  </tr>
                </ng-template>
                <ng-template pTemplate="emptymessage">
                  <tr>
                    <td colspan="6" class="tw-text-center tw-py-8 tw-text-gray-500">
                      Aucune soumission enregistrée.
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            </div>
          </p-tabPanel>

          <!-- ─── Onglet 4 : Patients ─── -->
          <p-tabPanel header="Patients">
            <div class="tw-pt-4 tw-space-y-4">
              @if (canEdit()) {
                <div class="tw-flex tw-justify-end">
                  <p-button
                    label="Ajouter un patient"
                    icon="pi pi-user-plus"
                    (onClick)="showPatientDialog.set(true)"
                  />
                </div>
              }

              <p-table
                [value]="patients()"
                [paginator]="true"
                [rows]="10"
                styleClass="tw-border tw-border-gray-200 tw-rounded-lg"
              >
                <ng-template pTemplate="header">
                  <tr>
                    <th>Code patient</th>
                    <th>Site</th>
                    <th>Date inclusion</th>
                    <th>Statut</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-patient>
                  <tr>
                    <td class="tw-font-mono tw-text-sm">{{ patient.patientCode }}</td>
                    <td class="tw-text-sm">{{ patient.siteCode ?? '—' }}</td>
                    <td class="tw-text-sm">
                      {{ patient.inclusionDate ? (patient.inclusionDate | date:'dd/MM/yyyy') : '—' }}
                    </td>
                    <td>
                      <p-tag [value]="patientStatusLabel(patient.status)" severity="info" />
                    </td>
                  </tr>
                </ng-template>
                <ng-template pTemplate="emptymessage">
                  <tr>
                    <td colspan="4" class="tw-text-center tw-py-8 tw-text-gray-500">
                      Aucun patient inscrit.
                    </td>
                  </tr>
                </ng-template>
              </p-table>

              <p class="tw-text-xs tw-text-gray-400 tw-text-center tw-flex tw-items-center tw-justify-center tw-gap-1">
                <i class="pi pi-shield"></i>
                Données pseudonymisées — conformité RGPD
              </p>
            </div>
          </p-tabPanel>

          <!-- ─── Onglet 5 : Historique ─── -->
          <p-tabPanel header="Historique">
            <div class="tw-pt-4">
              @if (statusHistory().length === 0) {
                <p class="tw-text-center tw-text-gray-500 tw-py-8">Aucun historique disponible.</p>
              } @else {
                <p-timeline [value]="statusHistory()" styleClass="tw-max-w-2xl">
                  <ng-template pTemplate="content" let-item>
                    <div class="tw-flex tw-flex-col tw-gap-1 tw-pb-6">
                      <div class="tw-flex tw-items-center tw-gap-2">
                        <ct-study-status-badge [status]="item.status" />
                        <span class="tw-text-sm tw-text-gray-500">
                          {{ item.statusDate | date:'dd/MM/yyyy' }}
                        </span>
                      </div>
                      @if (item.comment) {
                        <p class="tw-text-sm tw-text-gray-700 tw-mt-1">{{ item.comment }}</p>
                      }
                      <p class="tw-text-xs tw-text-gray-400">Par {{ item.changedBy }}</p>
                    </div>
                  </ng-template>
                  <ng-template pTemplate="marker" let-item>
                    <span class="tw-w-4 tw-h-4 tw-rounded-full tw-bg-blue-500 tw-block"></span>
                  </ng-template>
                </p-timeline>
              }
            </div>
          </p-tabPanel>

          <!-- ─── Onglet 6 : Documents ─── -->
          <p-tabPanel header="Documents">
            <div class="tw-flex tw-flex-col tw-items-center tw-justify-center tw-py-16 tw-gap-4 tw-text-gray-400">
              <i class="pi pi-folder-open tw-text-6xl"></i>
              <p class="tw-text-lg tw-font-medium">Module documents en développement</p>
              <p class="tw-text-sm">La gestion documentaire sera disponible dans une prochaine version.</p>
            </div>
          </p-tabPanel>

        </p-tabView>
      </div>
    }

    @if (!isLoading() && !study()) {
      <div class="tw-flex tw-flex-col tw-items-center tw-justify-center tw-py-16 tw-gap-4 tw-text-gray-400">
        <i class="pi pi-exclamation-triangle tw-text-6xl tw-text-yellow-500"></i>
        <p class="tw-text-lg tw-font-medium">Étude introuvable</p>
        <p-button label="Retour à la liste" icon="pi pi-arrow-left" (onClick)="goBack()" />
      </div>
    }

    <!-- ─── Dialog : Changement de statut ─── -->
    <p-dialog
      header="Modifier le statut de l'étude"
      [(visible)]="showStatusDialogVisible"
      [modal]="true"
      [style]="{width: '480px'}"
      (onHide)="resetStatusForm()"
    >
      <div class="tw-space-y-4 tw-pt-2">
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">Nouveau statut *</label>
          <p-dropdown
            [options]="studyStatusOptions"
            [(ngModel)]="statusForm.status"
            optionLabel="label"
            optionValue="value"
            placeholder="Sélectionner un statut"
            styleClass="tw-w-full"
          />
        </div>
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">Date de changement *</label>
          <input pInputText type="date" [(ngModel)]="statusForm.statusDate" class="tw-w-full" />
        </div>
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">Commentaire</label>
          <textarea pInputTextarea [(ngModel)]="statusForm.comment" rows="3" class="tw-w-full"
            placeholder="Motif du changement..."></textarea>
        </div>
      </div>
      <ng-template pTemplate="footer">
        <p-button label="Annuler" severity="secondary" (onClick)="showStatusDialog.set(false)" />
        <p-button
          label="Enregistrer"
          icon="pi pi-check"
          [disabled]="!statusForm.status || !statusForm.statusDate"
          (onClick)="submitStatusChange()"
        />
      </ng-template>
    </p-dialog>

    <!-- ─── Dialog : Ajout contact ─── -->
    <p-dialog
      header="Ajouter un contact"
      [(visible)]="showContactDialogVisible"
      [modal]="true"
      [style]="{width: '520px'}"
      (onHide)="resetContactForm()"
    >
      <div class="tw-space-y-4 tw-pt-2">
        <div class="tw-grid tw-grid-cols-2 tw-gap-4">
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Type *</label>
            <p-dropdown
              [options]="contactTypeOptions"
              [(ngModel)]="contactForm.contactType"
              optionLabel="label"
              optionValue="value"
              placeholder="Type de contact"
              styleClass="tw-w-full"
            />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Contact principal</label>
            <div class="tw-flex tw-items-center tw-gap-2 tw-pt-1">
              <p-checkbox [(ngModel)]="contactForm.isPrimary" [binary]="true" inputId="isPrimary" />
              <label for="isPrimary" class="tw-text-sm tw-cursor-pointer">Principal</label>
            </div>
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Prénom *</label>
            <input pInputText [(ngModel)]="contactForm.firstName" class="tw-w-full" />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Nom *</label>
            <input pInputText [(ngModel)]="contactForm.lastName" class="tw-w-full" />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Email</label>
            <input pInputText type="email" [(ngModel)]="contactForm.email" class="tw-w-full" />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Téléphone</label>
            <input pInputText [(ngModel)]="contactForm.phone" class="tw-w-full" />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1 tw-col-span-2">
            <label class="tw-text-sm tw-font-medium">Organisation</label>
            <input pInputText [(ngModel)]="contactForm.organization" class="tw-w-full" />
          </div>
        </div>
      </div>
      <ng-template pTemplate="footer">
        <p-button label="Annuler" severity="secondary" (onClick)="showContactDialog.set(false)" />
        <p-button
          label="Ajouter"
          icon="pi pi-check"
          [disabled]="!contactForm.contactType || !contactForm.firstName || !contactForm.lastName"
          (onClick)="submitContact()"
        />
      </ng-template>
    </p-dialog>

    <!-- ─── Dialog : Ajout soumission ─── -->
    <p-dialog
      header="Ajouter une soumission"
      [(visible)]="showSubmissionDialogVisible"
      [modal]="true"
      [style]="{width: '520px'}"
      (onHide)="resetSubmissionForm()"
    >
      <div class="tw-space-y-4 tw-pt-2">
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium">Type de soumission *</label>
          <p-dropdown
            [options]="submissionTypeOptions"
            [(ngModel)]="submissionForm.submissionType"
            optionLabel="label"
            optionValue="value"
            placeholder="Type..."
            styleClass="tw-w-full"
          />
        </div>
        <div class="tw-grid tw-grid-cols-2 tw-gap-4">
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Date de soumission *</label>
            <input pInputText type="date" [(ngModel)]="submissionForm.submissionDate" class="tw-w-full" />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Date limite</label>
            <input pInputText type="date" [(ngModel)]="submissionForm.dueDate" class="tw-w-full" />
          </div>
        </div>
        <div class="tw-grid tw-grid-cols-2 tw-gap-4">
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Soumis par</label>
            <input pInputText [(ngModel)]="submissionForm.submittedBy" class="tw-w-full" />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">N° de référence</label>
            <input pInputText [(ngModel)]="submissionForm.referenceNumber" class="tw-w-full" />
          </div>
        </div>
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium">Commentaires</label>
          <textarea pInputTextarea [(ngModel)]="submissionForm.comments" rows="3" class="tw-w-full"></textarea>
        </div>
      </div>
      <ng-template pTemplate="footer">
        <p-button label="Annuler" severity="secondary" (onClick)="showSubmissionDialog.set(false)" />
        <p-button
          label="Ajouter"
          icon="pi pi-check"
          [disabled]="!submissionForm.submissionType || !submissionForm.submissionDate"
          (onClick)="submitSubmission()"
        />
      </ng-template>
    </p-dialog>

    <!-- ─── Dialog : Ajout patient ─── -->
    <p-dialog
      header="Ajouter un patient"
      [(visible)]="showPatientDialogVisible"
      [modal]="true"
      [style]="{width: '480px'}"
      (onHide)="resetPatientForm()"
    >
      <div class="tw-space-y-4 tw-pt-2">
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium">Code patient (pseudonymisé) *</label>
          <input pInputText [(ngModel)]="patientForm.patientCode" placeholder="Ex: P001" class="tw-w-full" />
        </div>
        <div class="tw-grid tw-grid-cols-2 tw-gap-4">
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Code site</label>
            <input pInputText [(ngModel)]="patientForm.siteCode" class="tw-w-full" />
          </div>
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium">Date d'inclusion</label>
            <input pInputText type="date" [(ngModel)]="patientForm.inclusionDate" class="tw-w-full" />
          </div>
        </div>
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium">Statut</label>
          <p-dropdown
            [options]="patientStatusOptions"
            [(ngModel)]="patientForm.status"
            optionLabel="label"
            optionValue="value"
            placeholder="Statut patient"
            styleClass="tw-w-full"
          />
        </div>
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium">Notes</label>
          <textarea pInputTextarea [(ngModel)]="patientForm.notes" rows="3" class="tw-w-full"></textarea>
        </div>
      </div>
      <ng-template pTemplate="footer">
        <p-button label="Annuler" severity="secondary" (onClick)="showPatientDialog.set(false)" />
        <p-button
          label="Ajouter"
          icon="pi pi-check"
          [disabled]="!patientForm.patientCode"
          (onClick)="submitPatient()"
        />
      </ng-template>
    </p-dialog>
  `,
})
export class StudyDetailComponent implements OnInit {

  private readonly studyService   = inject(StudyService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly messageService = inject(MessageService);

  /** ID de l'étude extrait des paramètres de route. */
  private studyId!: string;

  // ─── Permissions RBAC ───────────────────────────────────────
  protected readonly canEdit = hasAnyRole(
    SystemRole.CTC_PM, SystemRole.CTC_CRA, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN
  );

  // ─── Options de dropdowns ───────────────────────────────────
  protected readonly studyStatusOptions    = STUDY_STATUS_OPTIONS;
  protected readonly contactTypeOptions    = CONTACT_TYPE_OPTIONS;
  protected readonly submissionTypeOptions = SUBMISSION_TYPE_OPTIONS;
  protected readonly patientStatusOptions  = PATIENT_STATUS_OPTIONS;

  // ─── État réactif (signals) ──────────────────────────────────
  /** Étude courante chargée depuis l'API. */
  protected readonly study          = signal<StudyResponse | null>(null);
  /** Indicateur de chargement principal. */
  protected readonly isLoading      = signal(false);
  /** Index de l'onglet actif (0-based). */
  protected activeTabIndex          = 0;
  /** Liste des contacts de l'étude. */
  protected readonly contacts       = signal<ContactResponse[]>([]);
  /** Liste des soumissions de l'étude. */
  protected readonly submissions    = signal<SubmissionResponse[]>([]);
  /** Liste des patients de l'étude. */
  protected readonly patients       = signal<PatientResponse[]>([]);
  /** Historique des statuts de l'étude. */
  protected readonly statusHistory  = signal<StudyStatusHistory[]>([]);

  // ─── Visibilité des dialogues ────────────────────────────────
  protected readonly showStatusDialog     = signal(false);
  protected readonly showContactDialog    = signal(false);
  protected readonly showSubmissionDialog = signal(false);
  protected readonly showPatientDialog    = signal(false);

  // ─── Bindings visibilité (deux-way avec p-dialog) ───────────
  get showStatusDialogVisible()     { return this.showStatusDialog();     }
  set showStatusDialogVisible(v: boolean) { this.showStatusDialog.set(v);     }

  get showContactDialogVisible()    { return this.showContactDialog();    }
  set showContactDialogVisible(v: boolean) { this.showContactDialog.set(v);    }

  get showSubmissionDialogVisible() { return this.showSubmissionDialog(); }
  set showSubmissionDialogVisible(v: boolean) { this.showSubmissionDialog.set(v); }

  get showPatientDialogVisible()    { return this.showPatientDialog();    }
  set showPatientDialogVisible(v: boolean) { this.showPatientDialog.set(v);    }

  // ─── Formulaires de saisie ──────────────────────────────────
  protected statusForm:     Partial<StatusUpdateRequest>  = {};
  protected contactForm:    Partial<ContactRequest>       = { isPrimary: false };
  protected submissionForm: Partial<SubmissionRequest>    = {};
  protected patientForm:    Partial<PatientRequest>       = {};

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.studyId = this.route.snapshot.paramMap.get('id')!;
    this.loadAll();
  }

  /**
   * Charge l'étude, les contacts, les soumissions, les patients et l'historique
   * en parallèle pour minimiser le temps de chargement.
   */
  private loadAll(): void {
    this.isLoading.set(true);

    Promise.all([
      this.studyService.getStudy(this.studyId).toPromise(),
      this.studyService.getContacts(this.studyId).toPromise(),
      this.studyService.getSubmissions(this.studyId).toPromise(),
      this.studyService.getPatients(this.studyId).toPromise(),
      this.studyService.getStatusHistory(this.studyId).toPromise(),
    ]).then(([study, contacts, submissionsPage, patientsPage, history]) => {
      if (study)            this.study.set(study);
      if (contacts)         this.contacts.set(contacts);
      if (submissionsPage)  this.submissions.set(submissionsPage.content);
      if (patientsPage)     this.patients.set(patientsPage.content);
      if (history)          this.statusHistory.set(history);
    }).catch((err) => {
      console.error('Erreur chargement étude', err);
      this.messageService.add({
        severity: 'error',
        summary: 'Erreur',
        detail: 'Impossible de charger les données de l\'étude.',
      });
    }).finally(() => {
      this.isLoading.set(false);
    });
  }

  /**
   * Calcule le pourcentage d'enrollment pour la barre de progression.
   *
   * @returns pourcentage entre 0 et 100
   */
  protected enrollmentPercent(): number {
    const s = this.study();
    if (!s || !s.targetEnrollment || s.targetEnrollment === 0) return 0;
    return Math.min(100, Math.round((s.currentEnrollment / s.targetEnrollment) * 100));
  }

  /**
   * Retourne le libellé français du statut de soumission.
   *
   * @param status statut brut
   */
  protected submissionStatusLabel(status: string): string {
    return SUBMISSION_STATUS_LABELS[status] ?? status;
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour un statut de soumission.
   *
   * @param status statut brut
   */
  protected submissionStatusSeverity(status: string): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    return SUBMISSION_STATUS_SEVERITY[status] ?? 'secondary';
  }

  /**
   * Retourne le libellé français du statut patient.
   *
   * @param status statut brut
   */
  protected patientStatusLabel(status: string): string {
    return PATIENT_STATUS_LABELS[status] ?? status;
  }

  // ─── Navigation ─────────────────────────────────────────────

  protected goBack(): void {
    this.router.navigate(['/studies']);
  }

  protected goToEdit(): void {
    this.router.navigate(['/studies', this.studyId, 'edit']);
  }

  // ─── Réinitialisation des formulaires ───────────────────────

  protected resetStatusForm(): void {
    this.statusForm = {};
  }

  protected resetContactForm(): void {
    this.contactForm = { isPrimary: false };
  }

  protected resetSubmissionForm(): void {
    this.submissionForm = {};
  }

  protected resetPatientForm(): void {
    this.patientForm = {};
  }

  // ─── Soumissions des formulaires ────────────────────────────

  /**
   * Soumet un changement de statut via PATCH /status.
   */
  protected submitStatusChange(): void {
    if (!this.statusForm.status || !this.statusForm.statusDate) return;

    const request: StatusUpdateRequest = {
      status:     this.statusForm.status,
      statusDate: this.statusForm.statusDate,
      comment:    this.statusForm.comment,
    };

    this.studyService.updateStatus(this.studyId, request).subscribe({
      next: (updated) => {
        this.study.set(updated);
        this.studyService.getStatusHistory(this.studyId).subscribe(h => this.statusHistory.set(h));
        this.showStatusDialog.set(false);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Statut mis à jour.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le statut.' });
      },
    });
  }

  /**
   * Ajoute un contact à l'étude.
   */
  protected submitContact(): void {
    if (!this.contactForm.contactType || !this.contactForm.firstName || !this.contactForm.lastName) return;

    const request: ContactRequest = {
      contactType:  this.contactForm.contactType,
      firstName:    this.contactForm.firstName,
      lastName:     this.contactForm.lastName,
      email:        this.contactForm.email,
      phone:        this.contactForm.phone,
      organization: this.contactForm.organization,
      isPrimary:    this.contactForm.isPrimary ?? false,
    };

    this.studyService.addContact(this.studyId, request).subscribe({
      next: (contact) => {
        this.contacts.update(list => [...list, contact]);
        this.showContactDialog.set(false);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contact ajouté.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'ajouter le contact.' });
      },
    });
  }

  /**
   * Supprime un contact de l'étude.
   *
   * @param contactId UUID du contact à supprimer
   */
  protected removeContact(contactId: string): void {
    this.studyService.removeContact(this.studyId, contactId).subscribe({
      next: () => {
        this.contacts.update(list => list.filter(c => c.id !== contactId));
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Contact supprimé.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de supprimer le contact.' });
      },
    });
  }

  /**
   * Ajoute une soumission réglementaire à l'étude.
   */
  protected submitSubmission(): void {
    if (!this.submissionForm.submissionType || !this.submissionForm.submissionDate) return;

    const request: SubmissionRequest = {
      submissionType: this.submissionForm.submissionType,
      submissionDate: this.submissionForm.submissionDate,
      dueDate:        this.submissionForm.dueDate,
      submittedBy:    this.submissionForm.submittedBy,
      referenceNumber: this.submissionForm.referenceNumber,
      comments:       this.submissionForm.comments,
    };

    this.studyService.addSubmission(this.studyId, request).subscribe({
      next: (sub) => {
        this.submissions.update(list => [...list, sub]);
        this.showSubmissionDialog.set(false);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Soumission ajoutée.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'ajouter la soumission.' });
      },
    });
  }

  /**
   * Inscrit un nouveau patient dans l'étude.
   */
  protected submitPatient(): void {
    if (!this.patientForm.patientCode) return;

    const request: PatientRequest = {
      patientCode:   this.patientForm.patientCode,
      inclusionDate: this.patientForm.inclusionDate,
      status:        this.patientForm.status,
      siteCode:      this.patientForm.siteCode,
      notes:         this.patientForm.notes,
    };

    this.studyService.addPatient(this.studyId, request).subscribe({
      next: (patient) => {
        this.patients.update(list => [...list, patient]);
        this.showPatientDialog.set(false);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Patient ajouté.' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'ajouter le patient.' });
      },
    });
  }
}
