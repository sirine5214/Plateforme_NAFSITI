import { Component, ElementRef, OnDestroy, input, output, signal, viewChild } from '@angular/core';

/**
 * Capture webcam pour la reconnaissance faciale.
 * La caméra ne s'allume qu'après un clic explicite et s'éteint dès que les captures sont faites.
 * Les images (JPEG en data URL) sont émises au parent ; rien n'est conservé dans le navigateur.
 */
@Component({
  selector: 'app-camera-capture',
  template: `
    <div class="nf-camera" [class.is-active]="active()">
      <video #video class="nf-camera__video" autoplay playsinline muted [hidden]="!active()" aria-label="Aperçu de la caméra"></video>
      @if (active()) {
        <div class="nf-camera__guide" aria-hidden="true"></div>
        @if (consigne()) {
          <div class="nf-camera__consigne" role="status">{{ consigne() }}</div>
        }
      } @else {
        <div class="nf-camera__placeholder">
          <i class="feather icon-camera"></i>
          <p>La caméra n'est activée que pendant la capture. Aucune image n'est conservée.</p>
        </div>
      }
    </div>

    @if (erreur()) {
      <div class="alert alert-danger nf-alert mt-3 mb-0" role="alert">
        <i class="feather icon-alert-circle"></i>
        <span>{{ erreur() }}</span>
      </div>
    }

    <div class="d-flex gap-2 mt-3">
      @if (!active()) {
        <button type="button" class="btn btn-light flex-fill" (click)="demarrer()" [disabled]="desactive()">
          <i class="feather icon-video me-1"></i> Activer la caméra
        </button>
      } @else {
        <button type="button" class="btn btn-light" (click)="arreter()" [disabled]="enCours()">Annuler</button>
        <button type="button" class="btn btn-primary flex-fill" (click)="capturer()" [disabled]="enCours() || desactive()">
          @if (enCours()) {
            <span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>
          }
          {{ libelle() }}
        </button>
      }
    </div>
  `
})
export class CameraCaptureComponent implements OnDestroy {
  /** Nombre d'images à prendre (plusieurs pour l'enregistrement, une pour la connexion). */
  readonly nbCaptures = input(1);
  readonly libelle = input('Capturer');
  readonly desactive = input(false);
  readonly captures = output<string[]>();

  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  private flux: MediaStream | null = null;

  readonly active = signal(false);
  readonly enCours = signal(false);
  readonly consigne = signal('');
  readonly erreur = signal('');

  async demarrer() {
    this.erreur.set('');
    if (!navigator.mediaDevices?.getUserMedia) {
      this.erreur.set("Votre navigateur ne permet pas d'utiliser la caméra (HTTPS ou localhost requis).");
      return;
    }
    try {
      this.flux = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } },
        audio: false
      });
      const v = this.video().nativeElement;
      v.srcObject = this.flux;
      await v.play();
      this.active.set(true);
      this.consigne.set('Placez votre visage dans le cadre, bien éclairé.');
    } catch {
      this.erreur.set("Accès à la caméra refusé ou impossible. Autorisez la caméra dans votre navigateur.");
      this.arreter();
    }
  }

  async capturer() {
    const n = Math.max(1, this.nbCaptures());
    const images: string[] = [];
    this.enCours.set(true);
    for (let i = 0; i < n; i++) {
      if (n > 1) {
        this.consigne.set(i === 0 ? 'Regardez la caméra…' : `Tournez très légèrement la tête… (${i + 1}/${n})`);
        await new Promise((r) => setTimeout(r, i === 0 ? 400 : 900));
      }
      const image = this.image();
      if (!image) {
        this.erreur.set("La caméra n'est pas prête, réessayez.");
        this.enCours.set(false);
        return;
      }
      images.push(image);
    }
    this.enCours.set(false);
    this.arreter();
    this.captures.emit(images);
  }

  arreter() {
    this.flux?.getTracks().forEach((t) => t.stop());
    this.flux = null;
    const v = this.video()?.nativeElement;
    if (v) v.srcObject = null;
    this.active.set(false);
    this.consigne.set('');
  }

  ngOnDestroy() {
    this.arreter();
  }

  private image(): string | null {
    const v = this.video().nativeElement;
    if (!v.videoWidth) return null;
    const canvas = document.createElement('canvas');
    canvas.width = v.videoWidth;
    canvas.height = v.videoHeight;
    canvas.getContext('2d')?.drawImage(v, 0, 0);
    return canvas.toDataURL('image/jpeg', 0.9);
  }
}
