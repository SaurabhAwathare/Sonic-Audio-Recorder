# Sonic Audio Recorder - Microphone & Audio Quality Testing Framework

This document outlines the rigorous testing procedures required to validate the acoustic performance and signal integrity of the Sonic Audio Recorder.

## 1. Environmental & Acoustic Test Matrix

| Test Scenario | Input Condition | Expected Result | Pass/Fail | Notes / Observations |
| :--- | :--- | :--- | :---: | :--- |
| **Silent Baseline** | Quiet room (<30dB ambient) | Hiss/System noise floor < -60dB FS. No CPU whine. | | |
| **Indoor Ambiance** | Moderate fan/AC noise | `NoiseSuppressor` attenuates hum without distorting voice. | | |
| **High Noise** | Simulated traffic/crowd | Voice remains intelligible; SNR > 15dB in loud environments. | | |
| **Soft Input** | Whisper at 30cm | AGC increases gain cleanly; no "pumping" artifacts. | | |
| **Normal Input** | Conversation at 1m | Natural frequency response; clear mid-range focus. | | |
| **Loud Input** | Shouting at 50cm | No digital clipping; peaks stay below -1dB FS. | | |
| **Proximity (0-5cm)** | Close speech / Plosives | No "pop" distortion or significant low-end mud. | | |
| **Distancy (>3m)** | Normal speech at distance | Intelligible recording; minimal "roominess" artifacts. | | |

## 2. Audio Quality Evaluation Criteria

For every recording captured during testing, evaluate against these engineering standards:

*   **Background Noise**: Check for system-induced hiss, hum, or "static" not present in the environment.
*   **Voice Clarity**: Assess intelligibility. Use the "STOI" (Short-Time Objective Intelligibility) logic: is every word clear?
*   **Clipping & Distortion**: Inspect the waveform in a tool like Audacity. Look for "flat tops" at 0.0dB.
*   **Volume Consistency**: Does the AGC (Automatic Gain Control) maintain a steady level without sudden jumps?
*   **Artifacts**: Listen for "metallic" sounds, digital crackling, or "underwater" effects from aggressive noise suppression.

## 3. Advanced Analysis (Tools: Audacity / Adobe Audition)

### Peak & RMS Levels
- **Target Peak**: -6dB to -3dB FS (Full Scale).
- **Target RMS**: -24dB to -18dB FS for consistent playback volume.

### Spectrogram Inspection
- **Sample Rate Check**: Verify a clear frequency response up to 24kHz (for 48kHz recordings).
- **Aliasing**: Ensure no "phantom" frequencies are mirrored back into the audible range.
- **DC Offset**: Waveform must be perfectly centered on the 0.0 horizontal line.

## 4. Debugging Technical Failures

If audio quality fails any test case, investigate the following:

| Symptom | Common Cause | Action |
| :--- | :--- | :--- |
| **"Underwater" Sound** | Aggressive Hardware NS | Toggle `noiseSuppressor?.enabled = false` in `AudioRecorder.kt`. |
| **Digital Clipping** | AGC Gain too high | Check `AutomaticGainControl` initialization or lower input source level. |
| **Crackling/Pops** | Buffer Underruns | Increase `bufferSize` multiplier in `AudioRecorder.kt`. |
| **Thin/Tinny Voice** | Wrong Audio Source | Verify `MediaRecorder.AudioSource.VOICE_RECOGNITION` is being used. |
| **Metallic Artifacts** | Low AAC Bitrate | Ensure AAC encoding is set to 192kbps or higher. |

## 5. Developer Logging
The following logs are implemented to help diagnose hardware-specific behavior:
- `AudioRecorder`: Hardware Effects status (NS/AGC availability).
- `AudioRecorder`: Start/Stop status and file format verification.
- `RecordingService`: Amplitude levels (captured for visualization).
