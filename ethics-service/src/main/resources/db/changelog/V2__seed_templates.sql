-- ============================================================
-- V2 : Templates de correspondance initiaux (globaux)
-- tenant_id = NULL → disponible pour tous les tenants
-- ============================================================

INSERT INTO correspondence_templates
    (id, template_code, template_name, template_type, language, is_active, subject, content, deleted, version, created_at, updated_at)
VALUES
(
    gen_random_uuid(),
    'APPROVAL_FR',
    'Lettre d''approbation (FR)',
    'APPROVAL_LETTER',
    'fr',
    true,
    'Avis favorable du Comité d''Éthique — [[${ethicsNumber}]]',
    '<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8"/>
  <title>Avis favorable CE</title>
  <style>
    body { font-family: Arial, sans-serif; font-size: 12pt; color: #333; margin: 40px; }
    .header { border-bottom: 2px solid #003366; margin-bottom: 20px; padding-bottom: 10px; }
    .footer { border-top: 1px solid #999; margin-top: 40px; padding-top: 10px; font-size: 10pt; color: #666; }
    .highlight { background-color: #f0f8ff; padding: 10px; border-left: 4px solid #003366; }
  </style>
</head>
<body>
  <div class="header">
    <h2>Comité d''Éthique — Cliniques Universitaires Saint-Luc</h2>
  </div>

  <p>Bruxelles, le <strong>[[${generatedDate}]]</strong></p>

  <p>À l''attention de <strong>[[${recipientName}]]</strong>,</p>

  <h3>Avis favorable du Comité d''Éthique</h3>

  <p>Nous avons le plaisir de vous informer que le Comité d''Éthique a examiné,
     lors de sa réunion, le protocole d''étude suivant :</p>

  <div class="highlight">
    <p><strong>Numéro CE :</strong> [[${ethicsNumber}]]</p>
    <p><strong>Titre :</strong> [[${studyTitle}]]</p>
    <p><strong>Acronyme :</strong> [[${acronym}]]</p>
    <p><strong>Investigateur principal :</strong> [[${investigatorName}]]</p>
    <p><strong>Promoteur :</strong> [[${sponsor}]]</p>
  </div>

  <p>Le Comité d''Éthique émet un <strong>avis favorable</strong> à la conduite de cette étude
     dans le respect du protocole soumis et de la réglementation en vigueur.</p>

  <p>Nous vous rappelons l''obligation de soumettre un rapport annuel au Comité
     dans les délais prévus, ainsi que de notifier tout événement indésirable grave.</p>

  <p>Veuillez agréer, [[${recipientName}]], l''expression de nos salutations distinguées.</p>

  <div class="footer">
    <p>Le Secrétariat du Comité d''Éthique<br/>
    Cliniques Universitaires Saint-Luc<br/>
    Avenue Hippocrate 10, 1200 Bruxelles</p>
  </div>
</body>
</html>',
    false,
    0,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'REJECTION_FR',
    'Lettre de refus (FR)',
    'REJECTION_LETTER',
    'fr',
    true,
    'Avis défavorable du Comité d''Éthique — [[${ethicsNumber}]]',
    '<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8"/>
  <title>Avis défavorable CE</title>
  <style>
    body { font-family: Arial, sans-serif; font-size: 12pt; color: #333; margin: 40px; }
    .header { border-bottom: 2px solid #cc0000; margin-bottom: 20px; padding-bottom: 10px; }
    .footer { border-top: 1px solid #999; margin-top: 40px; padding-top: 10px; font-size: 10pt; color: #666; }
    .highlight { background-color: #fff0f0; padding: 10px; border-left: 4px solid #cc0000; }
  </style>
</head>
<body>
  <div class="header">
    <h2>Comité d''Éthique — Cliniques Universitaires Saint-Luc</h2>
  </div>

  <p>Bruxelles, le <strong>[[${generatedDate}]]</strong></p>

  <p>À l''attention de <strong>[[${recipientName}]]</strong>,</p>

  <h3>Avis défavorable du Comité d''Éthique</h3>

  <p>Nous avons le regret de vous informer que le Comité d''Éthique a rendu un
     <strong>avis défavorable</strong> concernant le protocole suivant :</p>

  <div class="highlight">
    <p><strong>Numéro CE :</strong> [[${ethicsNumber}]]</p>
    <p><strong>Titre :</strong> [[${studyTitle}]]</p>
    <p><strong>Investigateur principal :</strong> [[${investigatorName}]]</p>
  </div>

  <p><strong>Motifs :</strong></p>
  <p>[[${comments}]]</p>

  <p>Vous avez la possibilité de soumettre une nouvelle demande tenant compte
     des observations du Comité.</p>

  <p>Veuillez agréer, [[${recipientName}]], l''expression de nos salutations distinguées.</p>

  <div class="footer">
    <p>Le Secrétariat du Comité d''Éthique<br/>
    Cliniques Universitaires Saint-Luc</p>
  </div>
</body>
</html>',
    false,
    0,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'MORE_INFO_FR',
    'Demande d''informations complémentaires (FR)',
    'MORE_INFO_LETTER',
    'fr',
    true,
    'Demande d''informations complémentaires — [[${ethicsNumber}]]',
    '<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8"/>
  <title>Demande d''informations CE</title>
  <style>
    body { font-family: Arial, sans-serif; font-size: 12pt; color: #333; margin: 40px; }
    .header { border-bottom: 2px solid #ff8c00; margin-bottom: 20px; padding-bottom: 10px; }
    .footer { border-top: 1px solid #999; margin-top: 40px; padding-top: 10px; font-size: 10pt; color: #666; }
    .highlight { background-color: #fffaf0; padding: 10px; border-left: 4px solid #ff8c00; }
  </style>
</head>
<body>
  <div class="header">
    <h2>Comité d''Éthique — Cliniques Universitaires Saint-Luc</h2>
  </div>

  <p>Bruxelles, le <strong>[[${generatedDate}]]</strong></p>

  <p>À l''attention de <strong>[[${recipientName}]]</strong>,</p>

  <h3>Demande d''informations complémentaires</h3>

  <p>Le Comité d''Éthique a examiné le protocole suivant :</p>

  <div class="highlight">
    <p><strong>Numéro CE :</strong> [[${ethicsNumber}]]</p>
    <p><strong>Titre :</strong> [[${studyTitle}]]</p>
    <p><strong>Investigateur principal :</strong> [[${investigatorName}]]</p>
  </div>

  <p>Avant de se prononcer, le Comité sollicite les informations complémentaires suivantes :</p>
  <p>[[${comments}]]</p>

  <p>Nous vous invitons à transmettre ces informations dans un délai de <strong>30 jours</strong>
     à l''adresse : <em>ethics@saintluc.be</em></p>

  <p>Veuillez agréer, [[${recipientName}]], l''expression de nos salutations distinguées.</p>

  <div class="footer">
    <p>Le Secrétariat du Comité d''Éthique<br/>
    Cliniques Universitaires Saint-Luc</p>
  </div>
</body>
</html>',
    false,
    0,
    NOW(),
    NOW()
),
(
    gen_random_uuid(),
    'ANNUAL_REMINDER_FR',
    'Rappel rapport annuel (FR)',
    'ANNUAL_REPORT_REQUEST',
    'fr',
    true,
    'Rappel : soumission du rapport annuel — [[${ethicsNumber}]]',
    '<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8"/>
  <title>Rappel rapport annuel CE</title>
  <style>
    body { font-family: Arial, sans-serif; font-size: 12pt; color: #333; margin: 40px; }
    .header { border-bottom: 2px solid #006400; margin-bottom: 20px; padding-bottom: 10px; }
    .footer { border-top: 1px solid #999; margin-top: 40px; padding-top: 10px; font-size: 10pt; color: #666; }
    .highlight { background-color: #f0fff0; padding: 10px; border-left: 4px solid #006400; }
    .urgent { color: #cc0000; font-weight: bold; }
  </style>
</head>
<body>
  <div class="header">
    <h2>Comité d''Éthique — Cliniques Universitaires Saint-Luc</h2>
  </div>

  <p>Bruxelles, le <strong>[[${generatedDate}]]</strong></p>

  <p>À l''attention de <strong>[[${recipientName}]]</strong>,</p>

  <h3>Rappel : Soumission du rapport annuel</h3>

  <p>Le Comité d''Éthique vous rappelle votre obligation de soumettre le rapport annuel
     de suivi pour l''étude suivante :</p>

  <div class="highlight">
    <p><strong>Numéro CE :</strong> [[${ethicsNumber}]]</p>
    <p><strong>Titre :</strong> [[${studyTitle}]]</p>
    <p><strong>Investigateur principal :</strong> [[${investigatorName}]]</p>
  </div>

  <p>Ce rapport annuel est <span class="urgent">requis dans les meilleurs délais</span>
     afin de permettre au Comité d''évaluer la poursuite de l''étude.</p>

  <p>Pour toute question, contactez le secrétariat à <em>ethics@saintluc.be</em></p>

  <p>Veuillez agréer, [[${recipientName}]], l''expression de nos salutations distinguées.</p>

  <div class="footer">
    <p>Le Secrétariat du Comité d''Éthique<br/>
    Cliniques Universitaires Saint-Luc<br/>
    Avenue Hippocrate 10, 1200 Bruxelles</p>
  </div>
</body>
</html>',
    false,
    0,
    NOW(),
    NOW()
);
