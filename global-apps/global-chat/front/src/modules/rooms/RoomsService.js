import crypto from 'crypto';
import Service from '../../common/Service';
import {ClientOTNode, OTStateManager} from "ot-core/lib";
import roomsOTSystem from "./ot/RoomsOTSystem";
import roomsSerializer from "./ot/serializer";
import RoomsOTOperation from "./ot/RoomsOTOperation";
import {randomString, wait, toEmoji} from '../../common/utils';

const RETRY_TIMEOUT = 1000;
const ROOM_ID_LENGTH = 32;

class RoomsService extends Service {
  constructor(roomsOTStateManager, contactsService, pubicKey) {
    super({
      rooms: new Map(),
      ready: false,
    });
    this._roomsOTStateManager = roomsOTStateManager;
    this._reconnectTimeout = null;
    this._contactsService = contactsService;
    this._myPublicKey = pubicKey;
    this._getRoomName.bind(this);
  }

  static createForm(contactsService, pubKey) {
    const roomsOTNode = ClientOTNode.createWithJsonKey({
      url: '/ot/rooms',
      serializer: roomsSerializer
    });
    const roomsOTStateManager = new OTStateManager(() => new Map(), roomsOTNode, roomsOTSystem);
    return new RoomsService(roomsOTStateManager, contactsService, pubKey);
  }

  async init() {
    try {
      await this._roomsOTStateManager.checkout();
    } catch (err) {
      console.log(err);
      await this._reconnectDelay();
      await this.init();
      return;
    }

    this._onStateChange();

    this._roomsOTStateManager.addChangeListener(this._onStateChange);
    this._contactsService.addChangeListener(this._onStateChange);
  }

  stop() {
    clearTimeout(this._reconnectTimeout);
    this._roomsOTStateManager.removeChangeListener(this._onStateChange);
    this._contactsService.removeChangeListener(this._onStateChange);
  }

  async createRoom(name, participants) {
    const roomId = randomString(ROOM_ID_LENGTH);
    await this._createRoom(roomId, name, [...participants, this._myPublicKey]);
  }

  async createDialog(participantId) {
    const participants = [this._myPublicKey, participantId];
    const roomId = this._getDialogRoomId(participants);
    const {name} = this._contactsService.state.contacts.get(participantId);

    let roomExists = false;
    [...this.state.rooms].map(([id, room]) => {
      if (id === roomId && !room.virtual) {
        roomExists = true;
      }
    });
    if (roomExists) {
      return;
    }
    await this._createRoom(roomId, name, participants);
  }

  async quitRoom(roomId) {
    const room = this.state.rooms.get(roomId);
    if (!room) {
      return;
    }

    const deleteRoomOperation = new RoomsOTOperation(roomId, room.participants, true);
    this._roomsOTStateManager.add([deleteRoomOperation]);
    await this._sync();
  }

  async _createRoom(roomId, name, participants) {
    const addRoomOperation = new RoomsOTOperation(roomId, participants, false);
    this._roomsOTStateManager.add([addRoomOperation]);
    await this._sync();
  }

  _onStateChange = () => {
    this.setState({
      rooms: this._getRooms(),
      ready: true
    });
  };

  _getDialogRoomId(participants) {
    return crypto
      .createHash('sha256')
      .update(participants.sort().join(';'))
      .digest('hex');
  }

  _getRoomName(room) {
    return room.participants
      .filter(participantPublicKey => participantPublicKey !== this._myPublicKey)
      .map(participantPublicKey => {
        return this._contactsService.getContactName(participantPublicKey) || toEmoji(participantPublicKey, 3);
      })
      .join(', ');
  }

  _getRooms() {
    let contacts = [...this._contactsService.getAll().contacts];
    const otState = [...this._roomsOTStateManager.getState()]
      .map(([roomId, room]) => {
        return {
          id: roomId,
          name: this._getRoomName(room),
          participants: room.participants,
          virtual: false,
          dialog: room.participants.length === 2 && roomId === this._getDialogRoomId(room.participants) &&
            this._roomsOTStateManager.getState().contacts
              .get(room.participants
                .filter(pubKey => pubKey !== this._myPublicKey))
        }
      });
    contacts = contacts.map(([contactPublicKey, contact]) => {
      const participants = [this._myPublicKey, contactPublicKey];
      return {
        id: this._getDialogRoomId(participants),
        name: contact.name,
        participants,
        virtual: true,
        dialog: true
      };
    });

    return new Map([
      ...contacts,
      ...otState
    ].map(({id, name, participants, virtual, dialog}) => ([id, {name, participants, virtual, dialog}])));
  }

  _reconnectDelay() {
    return new Promise(resolve => {
      this._reconnectTimeout = setTimeout(resolve, RETRY_TIMEOUT);
    });
  }

  async _sync() {
    try {
      await this._roomsOTStateManager.sync();
    } catch (err) {
      console.log(err);
      await wait(RETRY_TIMEOUT);
      await this._sync();
    }
  }
}

export default RoomsService;
