# Release signing
Debug uses the standard Android local debug identity. No source key is copied.
Release outputs are unsigned, including the bundle, until the new app wires its own signing config.
Copy `base-release-signing.properties.example` to an ignored local file. Supply passwords via CI
secrets or local protected properties. Do not put signing passwords in BuildConfig or environment examples.
Validate files/aliases only for the selected release signing task, never during debug configuration.
An unsigned AAB is not upload-ready. Preserve the upload identity and Play App Signing separately.
