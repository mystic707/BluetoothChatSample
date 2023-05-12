# BluetoothSample

빌드 테스트 환경
- 2023.05 ~
- Android Studio Electric Eel 2022.1.1 Patch 2
- Android 12 Device

애플리케이션 실행 후 UI 노출 결과에 따른 기능
- 권한 요청
  - 블루투스 기능을 사용하기 위한 권한 요청
  - 다른 기능 사용 시 필수 조건
- 블루투스 활성
  - 블루투스 활성화 (블루투스 어댑터 활성)
  - 다른 기능 사용 시 필수 조건
- 블루투스 기기 확인 및 연결
  - 이미 페어링 하였던 기기 목록과 신규 연결 가능한 근처 단말 목록을 dialog로 노출
  - 디바이스 선택 시 연결 시도
- 블루투스 연결 상태 확인
  - Not Connected, Listen, Connecting, Connected 4가지 상태로 노출됨
    - Not Connected : 연결되지 않음 (아무 소켓도 열려있지 않음)
    - Listen : 언결되지 않았으나 나에게 연결 시도 시 accept 할 수 있는 소켓이 열려있음
    - Connecting : 연결 진행중
    - Connected : 연결되었음
- 메시지 보내기 / 메시지 기록
  - 양쪽 디바이스가 모두 Connected 된 상태라면 메시지 보내기 / 메시지 기록을 확인할 수 있음