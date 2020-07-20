export interface ConfirmationConfig {
  verify?: ConfirmationConfigVerify;
  text: ConfirmationConfigText;
}

export interface ConfirmationConfigText {
  title: string;
  info: string;
  ctaButtonLabel: string;
}

export interface ConfirmationConfigVerify {
  inputLabel: string;
  compareValue: string;
}
