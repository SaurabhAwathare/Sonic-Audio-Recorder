# Sonic Audio Recorder - QA Testing Checklist

This checklist is designed for validating the core functionality of the Sonic Audio Recorder app.

## 1. Core Functionality Testing
| ID | Test Case Description | Expected Result | Pass/Fail | Notes |
| :--- | :--- | :--- | :--- | :--- |
| 1.1 | Start recording from Idle | Recording starts < 200ms; UI timer begins; DND triggers. | | |
| 1.2 | Pause recording | Waveform/Timer freezes; File stream holds; No "pop" sound. | | |
| 1.3 | Resume recording | Recording continues seamlessly; no audio gap. | | |
| 1.4 | Stop and Save | Recording terminates; File saved; Entry in list; DND restored. | | |

## 2. File & Metadata Validation
| ID | Test Case Description | Expected Result | Pass/Fail | Notes |
| :--- | :--- | :--- | :--- | :--- |
| 2.1 | File Naming | File name follows timestamp-based pattern. | | |
| 2.2 | Storage Location | File is saved in internal app directory (or specified path). | | |
| 2.3 | Format Verification | Header is valid (WAV/MP3); Bitrate is consistent. | | |

## 3. Playback Validation
| ID | Test Case Description | Expected Result | Pass/Fail | Notes |
| :--- | :--- | :--- | :--- | :--- |
| 3.1 | Immediate Playback | Recorded file plays back immediately after saving. | | |
| 3.2 | Seek Control | Scrubbing seek bar updates position without lag. | | |
| 3.3 | Audio Quality | No distortion, clipping, or introduced background hiss. | | |

## 4. Duration & Stress Testing
| ID | Test Case Description | Expected Result | Pass/Fail | Notes |
| :--- | :--- | :--- | :--- | :--- |
| 4.1 | Short recording (~2s) | Saved correctly; not corrupted; playable. | | |
| 4.2 | Long recording (15m+) | App stable; No OOM; Foreground service stays active. | | |
| 4.3 | Battery/Thermal | CPU < 10%; No significant device heating. | | |

## 5. Edge Cases & Interruptions
| ID | Test Case Description | Expected Result | Pass/Fail | Notes |
| :--- | :--- | :--- | :--- | :--- |
| 5.1 | Incoming Phone Call | Recording pauses (or handles focus loss); Resumes after. | | |
| 5.2 | Low Storage (<50MB) | Recording stops gracefully with user alert. | | |
| 5.3 | Permission Revoke | Revoke Mic/DND mid-use; App handles SecurityException. | | |
| 5.4 | Device Shutdown | Battery dies mid-recording; Check if file is "fixed". | | |

---

## Logging & Debugging
The following logs have been implemented to assist with debugging:
- **AudioRecorder**: `Log.i` for start/stop, `Log.d` for effects and header updates.
- **AudioStateManager**: `Log.d` for DND state transitions.
- **AudioPlayer**: `Log.d` for playback state and errors.
- **RecordingService**: `Log.i` for lifecycle events and DB saving.

## Automation Plan
1. **Unit Tests**: `AudioStateManagerTest` to verify DND logic.
2. **Integration Tests**: Espresso tests for the Start/Pause/Stop flow.
3. **Database Tests**: Verify Room entity insertion after saving.
