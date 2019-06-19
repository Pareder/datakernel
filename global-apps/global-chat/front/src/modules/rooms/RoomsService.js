import Service from '../../common/Service';
import RoomsOTOperation from "./ot/RoomsOTOperation";

const RETRY_CHECKOUT_TIMEOUT = 1000;

class RoomsService extends Service {
  constructor(roomsOTStateManager, messagingURL) {
    super({
      rooms: [],
      ready: false,
    });
    this._roomsOTStateManager = roomsOTStateManager;
    this._reconnectTimeout = null;
    this._messagingURL = messagingURL;
  }

  async init() {
    // Get initial state
    try {
      await this._roomsOTStateManager.checkout();
    } catch (err) {
      console.error(err);
      await this._reconnectDelay();
      await this.init();
      return;
    }

    this._onStateChange();

    this._roomsOTStateManager.addChangeListener(this._onStateChange);
  }

  stop() {
    clearTimeout(this._reconnectTimeout);
    this._roomsOTStateManager.removeChangeListener(this._onStateChange);
  }

  async createRoom(participants) {
    const addRoomOperation = new RoomsOTOperation([{
      id: Math.random().toString(16).substr(2),
      participants: participants,
      remove: false
    }]);
    this._roomsOTStateManager.add([addRoomOperation]);
    await this._sync();
  }

  async quitRoom(id) {
    let room = this.state.rooms.find(room => room.id === id);
    if (!room){
      throw new Error("Unknown room ID");
    }
    const removeRoomOperation = new RoomsOTOperation([{
      id: id,
      participants: room.participants,
      remove: true
    }]);
    this._roomsOTStateManager.add([removeRoomOperation]);
    await this._sync();
  }

  _onStateChange = () => {
    this.setState({
      rooms: this._getRoomsFromStateManager(),
      ready: true
    });
  };

  _getRoomsFromStateManager() {
    return Array.from(this._roomsOTStateManager.getState())
      .sort()
      .map(entry => {
        return {
          id: entry[0],
          participants: entry[1].participants
        };
      });
  }

  _reconnectDelay() {
    return new Promise(resolve => {
      this._reconnectTimeout = setTimeout(resolve, RETRY_CHECKOUT_TIMEOUT);
    });
  }

  async _sync() {
    try {
      await this._roomsOTStateManager.sync();
    } catch (err) {
      console.error(err);
      await this._sync();
    }
  }
}

export default RoomsService;
