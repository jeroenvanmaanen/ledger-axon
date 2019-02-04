import React, { Component } from 'react';
import REST from './rest-client';
import Period from './Period';
import Export from './Export';

class Compound extends Component {
  constructor(props) {
    super(props);
    this.handlePrefixChange = this.handlePrefixChange.bind(this);
    this.handleExportChange = this.handleExportChange.bind(this);
    this.handleFocusChange = this.handleFocusChange.bind(this);
    this.handleLabelChange = this.handleLabelChange.bind(this);
    this.intendedJarChange = this.intendedJarChange.bind(this);
    this.changeIntendedJar = this.changeIntendedJar.bind(this);
    this.handleJarChangeFunction = this.handleJarChangeFunction.bind(this);
    this.handleContentToggle = this.handleContentToggle.bind(this);
    this.getAccounts = this.getAccounts.bind(this);
    this.isMember = this.isMember.bind(this);
    this.state = {
      _id: '',
      label: '',
      transactions: [],
      balance: {},
      balanceValid: '',
      jars: '',
      intendedJar: '*',
      compoundId: undefined,
      compoundRef: '',
      staticAccounts: {},
      staticPrefix: props.initialPrefix || '',
      staticTransactions: [],
      staticExport: false
    };
    this.fresh();
  }

  render() {
    const compoundApi = {
        changeFocus: this.handleFocusChange,
        isMember: this.isMember,
        getAccounts: this.getAccounts
    };
    const prefixLabel = this.state.staticPrefix ? this.state.staticPrefix + '*' : '???';
    const periodLabel = 'Period ' + prefixLabel;
    console.log('Balance:', this.state.balance);
    console.log('Number of transactions', this.state.staticTransactions.length);
    return (
        <div className="CompoundContainer">
            <div className="Compound">
              <div className="content">
                <h2>Compound [{this.state.jars}]</h2>
                <div><span>Intended jar:</span>
                  <div className='compoundJars'>
                    <div className='affected'>
                      {Object.keys(this.state.staticAccounts)
                        .map((accountNr) => {
                          const account = this.state.staticAccounts[accountNr];
                          if (this.state.balance[account.key]) {
                            var cssClass = 'compoundJar';
                            var handler = this.handleJarChangeFunction(account.key);
                            if (account.key === this.state.intendedJar) {
                              cssClass = cssClass + ' intended';
                              handler = this.handleJarChangeFunction('?');
                              if (this.state.balanceValid === 'yes') {
                                cssClass = cssClass + ' valid';
                              }
                            }
                            return (<div key={account.key} className={cssClass} onClick={handler}>{account.key}</div>)
                          } else {
                            return null;
                          }
                        })
                      }
                    </div>
                    <div className='unaffected'>
                      <div className='expandHandle'>&gt;</div>
                      {Object.keys(this.state.staticAccounts)
                        .map((accountNr) => {
                          const account = this.state.staticAccounts[accountNr];
                          if (!this.state.balance[account.key]) {
                            var cssClass = 'compoundJar';
                            var handler = this.handleJarChangeFunction(account.key);
                            if (account.key === this.state.intendedJar) {
                              cssClass = cssClass + ' intended';
                            }
                            return (<div key={account.key} className={cssClass} onClick={handler}>{account.key}</div>)
                          } else {
                            return null;
                          }
                        })
                      }
                      <div className='compoundJar' onClick={this.handleJarChangeFunction(this.state.intendedJar === '*' ? '?' : '*')}>*</div>
                    </div>
                  </div>
                </div>
                <p>Label:</p>
                <p><input type="text" name="label" className="compoundInput" value={this.state.label} onChange={this.handleLabelChange} /></p>
                <p>Balance:</p>
                <ul>
                    {Object.keys(this.state.balance).map((jar) => {
                        return (<li key={jar}>{jar}: {this.state.balance[jar]}</li>);
                    })}
                </ul>
                <div>
                  <div className="content">
                    <p>Transactions:</p>
                    <ul>
                        {this.state.transactions.map((transaction) => {
                            return (<li key={transaction.id}>{transaction.id}: {transaction.amountCents} {transaction.jar} {transaction.contraJar}</li>);
                        })}
                    </ul>
                  </div>
                  <div className="toggle" onClick={this.handleContentToggle}>^</div>
                </div>
              </div>
              <div className="toggle" onClick={this.handleContentToggle}>^</div>
            </div>
            <div>
              <form>
                <p><input type="text" onChange={this.handlePrefixChange}/></p>
                <p>Show export <input type="checkbox" defaultChecked={this.state.staticExport} onClick={this.handleExportChange}/></p>
              </form>
              {this.state.staticExport
                ? (<Export label={periodLabel} compoundApi={compoundApi} transactions={this.state.staticTransactions} />)
                : (<Period label={periodLabel} compoundApi={compoundApi} transactions={this.state.staticTransactions} />)
              }
            </div>
        </div>
    );
  }

  handleContentToggle(event) {
    const target = event.target;
    console.log('Handle content toggle: target:', target);
    const parent = target.parentNode;
    console.log('Handle content toggle: parent:', parent);
    parent.childNodes.forEach((child) => {
      var childClass = ' ' + (child.getAttribute('class') || '') + ' ';
      if (childClass.match(/ content /)) {
        if (childClass.match(/ hidden /)) {
          childClass = childClass.replace(/ hidden /g, ' ');
        } else {
          childClass = childClass + 'hidden ';
        }
        child.setAttribute('class', childClass.trim());
      }
    });
  }

  handleExportChange(event) {
    const target = event.target;
    console.log('Handle export change:', target, target.checked);
    this.setState({staticExport: target.checked});
  }

  handleJarChangeFunction(intendedJar) {
    return () => {
      this.changeIntendedJar(intendedJar);
    };
  }

  async fresh() {
    await this.refreshAccounts();
    await this.refreshTransactions(this.state.staticPrefix);
  }

  async refreshAccounts() {
    const self = this;
    const accountsPromise = await REST('/api/accounts')
    console.log('accountsPromise', accountsPromise);
    var accountsMap = {};
    var record;
    var index;
    for (index = 0; index < accountsPromise.entity.length; index++) {
      record = accountsPromise.entity[index];
      accountsMap[record.account] = record;
    }
    console.log('accountsMap', accountsMap);
    self.setState({staticAccounts: accountsMap});
  }

  getAccounts() {
    return this.state.staticAccounts;
  }

  async refreshTransactions(prefix) {
    const self = this;
    if (prefix.length >= 4) {
      const dataPromise = await REST({method:'POST', path:'/api/entries/date-prefix?datePrefix=' + prefix});
      console.log('dataPromise', dataPromise);
      dataPromise.entity.forEach(record => {
        record.jar = self.getJarFromRecord(record);
      });
      self.setState({staticTransactions: dataPromise.entity});
    }
  }

  getJarFromRecord(record) {
    var accounts = this.state.staticAccounts;
    var value = record.account;
    if (accounts && accounts.hasOwnProperty(value)) {
      return accounts[value].key;
    } else {
      value = record.contraAccount;
      if (accounts && accounts.hasOwnProperty(value)) {
        return accounts[value].key;
      } else {
        return '*';
      }
    }
  }

  handlePrefixChange(event) {
    const prefix = event.target.value;
    this.setState({ staticPrefix: prefix });
    this.refreshTransactions(prefix);
  }

  handleLabelChange(event) {
    const target = event.target;
    this.setState({ label: target.value });
    this.saveState({ label: target.value });
  }

  intendedJarChange(event) {
    const target = event.target;
    this.changeIntendedJar(target.value);
  }

  changeIntendedJar(intendedJar) {
    this.setState({ intendedJar: intendedJar });
    this.saveState({ intendedJar: intendedJar, balanceValid: this.isBalanceValid(this.state.jars, intendedJar) });
  }

  findTransactionElement(event) {
    var target = event.target;
    var toggle = false;
    var jar = undefined;
    var j;
    while (!this.hasClass(target, 'transaction') && target.parentNode && target.tagName !== 'TABLE') {
        console.log('Handle focus change: target parent:', target, target.tagName, target.className, target.parentNode);
        if (target.className === 'key_add') {
            toggle = true;
        }
        j = target.getAttribute('jar');
        if (j) {
          jar = j;
        }
        target = target.parentNode;
    }
    return {
      target: target,
      toggle: toggle,
      jar: jar
    }
  }

  handleFocusChange(event) {
    const transactionElement = this.findTransactionElement(event);
    const target = transactionElement.target;
    const toggle = transactionElement.toggle;
    const jar = transactionElement.jar;
    console.log('Handle focus change: target:', target, target.tagName, target.className, target.parentNode);
    if (this.hasClass(target, 'transaction') && target.getAttribute('data-id')) {
        const sameCompound = this.contains(this.state, {key:target.getAttribute('data-key')});
        if (sameCompound && jar && jar !== this.state.intendedJar) {
          this.changeIntendedJar(jar)
        } else {
          this.updateState(target, toggle);
        }
    }
  }

  async updateState(target, toggle) {
    const transactionId = target.getAttribute('data-id');
    const transactionKey = target.getAttribute('data-key');
    const date = this.getField(target, 'date');
    const amount = this.getField(target, 'amountCents');
    const jar = this.getJar(target, 'account');
    const contraJar = this.getJar(target, 'contraAccount');
    const transaction = {
        _id: transactionId,
        key: transactionKey,
        date: date,
        amount: amount,
        jar: jar,
        contraJar: contraJar
    };
    const transactionRef = {
        _id: transactionId,
        key: transactionKey
    };
    var stateChange = {};
    var transactions;
    if(toggle) {
        console.log('Toggle');
        transactions = this.state.transactions;
        if (transactions.some(t => t.key === transactionKey)) {
            console.log('Remove');
            transactions = transactions.filter(t => t.key !== transactionKey);
            if (transactions.length === 1 && this.state.compoundId) {
                await this.deleteCompound(this.getRef(transactions[0]), this.state.compoundId);
                stateChange.compoundId = undefined;
            }
        } else {
            console.log('Add');
            transactions.push(transaction);
            if (transactions.length === 2) {
              console.log('Add compound:', transactions)
              const newState = this.copyState();
              newState.transactions = transactions;
              const compoundId = await this.insertCompound(this.getRef(transactions[0]), newState);
              stateChange.compoundId = compoundId;
            }
        }
    } else {
        console.log('Singleton:', transaction._id, transaction);
        transactions = [ transaction ];
        stateChange._id = '';
        stateChange.label = '';
        stateChange.compoundId = '';
        stateChange.compoundRef = '';
        stateChange.intendedJar = transaction.intendedJar;
        stateChange.balanceValid = transaction.balanceValid;
        const entry = await this.getEntry({key: transaction._id})
        const compoundId = entry.compoundId;
        if (compoundId) {
            const result = await REST('/api/compound/' + compoundId);
            const compound = result.entity;
            if (compound.members) {
                console.log('Retrieved compound transaction:', compound);
                stateChange.compoundId = compoundId;
                stateChange.transactions = this.asList(compound.members);
                console.log('State change transactions:', stateChange.transactions);
                stateChange.compoundRef = this.getCompoundRef(stateChange.transactions);
                stateChange.label = compound.label || '';
                stateChange.intendedJar = compound.intendedJar;
                stateChange.balance = compound.balance;
                stateChange.balanceValid = compound.balanceMatchesIntention;
                transactions = stateChange.transactions;
            }
        }
    }
    stateChange.transactions = transactions;
    console.log('State change:', stateChange);
    this.setState(stateChange);
    this.updateBalance(stateChange, toggle, transactionRef);
  }

  updateBalance(newState, toggle, transactionRef) {
    var newBalance = newState.balance;
    if (!newBalance) {
        newBalance = {};
        newState.balance = newBalance;
    }
    this.setState({ balance: newBalance });
    var jars = [];
    Object.keys(newBalance).forEach((key) => {
        if (!key || key === '*') {
            return;
        }
        if (!newBalance[key]) {
            return;
        }
        jars.push(key);
    });
    newState.jars = jars;
    const balanceValid = this.isBalanceValid(jars.join(), this.state.intendedJar);
    const update = (this.state.balanceValid === 'yes') !== (balanceValid === 'yes');
    console.log('Balance valid:', jars.join(), this.state.intendedJar, balanceValid, update);
    if (update) {
        newState.balanceValid = balanceValid;
    }
    newState.compoundRef = this.getCompoundRef(newState.transactions);
    this.setState({ jars: jars.join(), balanceValid: balanceValid, compoundRef: newState.compoundRef });
    if (toggle || update) {
        this.saveState(newState, transactionRef)
    }
  }

  isBalanceValid(jars, intendedJar) {
    return (jars === intendedJar) ? 'yes' : 'no';
  }

  getField(node, key) {
    var result = undefined;
    node.childNodes.forEach(child => {
        if (child.className === 'key_' + key) {
            var unformatted = child.getAttribute('unformatted');
            if (unformatted === undefined) {
                result = child.textContent;
            } else {
                result = unformatted;
            }
        }
    });
    return result;
  }

  getJar(node, key) {
    var result = undefined;
    node.childNodes.forEach(child => {
        if (child.className === 'key_' + key) {
            result = child.getAttribute('jar');
        }
    });
    return result;
  }

  isMember(transactionKey) {
    var result = false;
    this.state.transactions.forEach(transaction => {
        const key = transaction && transaction.key;
        if (key === transactionKey) {
            result = true;
        }
    });
    return result;
  }

  hasClass(node, className) {
    const nodeClass = node.className;
    const result = !!(' ' + nodeClass + ' ').match(' ' + className + ' ');
    console.log('Has class:', nodeClass, className, result);
    return result;
  }

  async saveState(update, transactionRef) {
    // console.log('Compound: save state:', this.state, update);
    var newState = this.copyState();
    Object.keys(update).forEach((key) => {
        newState[key] = update[key];
    });
    const isCompound = newState.transactions.length > 1;
    if (isCompound) {
        console.log('Compound: update new state:', newState);
        if (transactionRef) {
            if (this.contains(newState, transactionRef)) {
                console.log('Compound: link transaction:', transactionRef.key);
                this.linkTransaction(transactionRef, newState);
            } else {
                console.log('Compound: unlink transaction:', transactionRef.key);
                this.unlinkTransaction(transactionRef);
            }
        }
        this.saveLabel(newState);
        this.updateTransactions(update, newState, !!transactionRef);
    } else {
        if (transactionRef) {
            if (this.contains(newState, transactionRef)) {
                console.log('Compound: load:', transactionRef.key);
            } else {
                console.log('Compound: delete:', transactionRef.key);
                this.unlinkTransaction(transactionRef);
                this.updateTransactions(update, newState);
            }
        } else if (newState.transactions.length === 1) {
            this.saveLabel(newState, this.getRef(newState.transactions[0]));
            this.updateTransactions(update, newState);
        }
    }
  }

  updateTransactions(update, newState, force) {
    if (update.intendedJar || update.balanceValid || update.label || force) {
        newState.transactions.forEach((transaction) => {
            console.log('Update transactions: saveTransaction:', transaction);
            this.saveTransaction(newState, this.getRef(transaction));
        });
        this.state.staticTransactions.forEach((transaction) => {
            if (this.contains(newState, this.getRef(transaction))) {
                transaction.intendedJar = newState.intendedJar;
                transaction.balanceValid = newState.balanceValid;
                transaction.compoundRef = newState.compoundRef;
                console.log('Updated transaction:', transaction);
            }
        });
        this.setState({staticTransactions: this.state.staticTransactions});
    } else {
        console.log('Skipped update transactions');
    }
  }

  contains(state, transactionRef) {
    return state.transactions.some((t) => {
      return t.key === transactionRef.key;
    });
  }

  async getEntry(transactionRef) {
    const result = await REST({
      path: '/api/entry/' + transactionRef.key
    });
    console.log('Get entry: result', result);
    return result.entity;
  }

  async getLabel(transactionRef) {
    const result = await REST({
      path: '/api/label?transactionKey="' + transactionRef.key + '"'
    });
    // console.log('Get label: result', result);
    const entities = result.entity
    return entities.length < 1 ? undefined : entities[0];
  }

  async saveTransaction(newState, transactionRef) {
    console.log('Save transaction:', transactionRef);
    const compoundId = newState.compoundId || this.state.compoundId;
    var newLabel = {
      transactionId: transactionRef._id,
      transactionKey: transactionRef.key
    };
    newLabel.compoundId = compoundId;
    newLabel.label = newState.label;
    newLabel.intendedJar = newState.intendedJar;
    newLabel.balanceValid = newState.balanceValid;
    newLabel.compoundRef = newState.compoundRef;
    console.log("New label:", newLabel);
    // TODO
  }

  async saveLabel(newState, transactionRef) {
    console.log('Save label:', newState.label, newState.transactions, transactionRef); // TODO
  }

  addToPatch(patch, key, value) {
    if (value) {
      patch[key] = value;
    }
  }

  getCompoundRef(compoundTransactions) {
    const external = compoundTransactions.filter((transaction) => transaction.contraJar === '*');
    if (external[0]) {
      var candidateRef = this.getRef(external[0]);
      external.forEach((transaction) => {
        const transactionRef = this.getRef(transaction);
        if (this.compareRefs(transactionRef, candidateRef) < 0) {
          candidateRef = transactionRef;
        }
      });
      return candidateRef.key;
    } else {
      return '';
    }
  }

  getRef(transaction) {
    const key = transaction.key;
    const pair = key.split('_');
    return {
      _id: transaction._id,
      key: transaction.key,
      date: pair[0],
      nr: parseInt(pair[1], 10)
    };
  }

  compareRefs(left, right) {
    if (left.date < right.date) {
      return -1;
    } else if (left.date > right.date) {
      return 1;
    } else {
      return Math.sign(left.nr - right.nr);
    }
  }

  summarize(transactions) {
    if (!transactions) {
       return;
    }
    var transactionSummaries = []
    transactions.forEach(transaction => {
      transactionSummaries.push({
        _id: transaction._id,
        key: transaction.key,
        date: transaction.date,
        jar: transaction.jar,
        contraJar: transaction.contraJar,
        amount: transaction.amount
      });
    });
    return transactionSummaries;
  }

  asList(transactions) {
    var result = []
    Object.keys(transactions).forEach((key) => {
        result.push(transactions[key]);
    });
    console.log('Transactions list:', result);
    return result;
  }

  async getLabelId(transactionRef) {
    const label = await this.getLabel(transactionRef);
    console.log("Get label ID:", transactionRef, label);
    return label ? label.id : transactionRef._id;
  }

  async insertCompound(transactionRef, newState) {
    var compound = {};
    compound.label = newState.label;
    compound.transactions = this.summarize(newState.transactions);
    compound.balance = newState.balance;
    const result = await REST({
      method: 'POST',
      path: '/api/compound',
      headers: {
        'Content-Type': 'application/json'
      },
      entity: compound
    });
    console.log("Insert compound", result);
    const compoundId = result.entity.id;
    newState.compoundId = compoundId;
    await this.linkTransaction(transactionRef, newState);
    this.setState({compoundId: compoundId})
    return compoundId;
  }

  async deleteCompound(transactionRef, compoundId) {
    if (compoundId) {
        var newState = this.copyState();
        newState.compoundId = undefined;
        this.saveLabel(newState, transactionRef);
        await REST({
          method: 'DELETE',
          path: '/api/compound/' + compoundId
        });
    }
  }

  async linkTransaction(transactionRef, newState) {
    const state = newState || this.state;
    const compoundId = state.compoundId;
    if (!compoundId) {
      return;
    }
    await this.saveLabel(newState, transactionRef);
  }

  async unlinkTransaction(transactionRef) {
    console.log('Unlink transaction') // TODO
    this.state.staticTransactions.forEach((transaction) => {
      if (transaction.key === transactionRef.key) {
        transaction.label = '';
        transaction.intendedJar = '';
        transaction.balanceValid = '';
        transaction.compoundRef = '';
      }
    });
    this.setState({staticTransactions: this.state.staticTransactions});
  }

  copyState() {
    var newState = {};
    Object.keys(this.state).forEach((key) => {
        if (!key.match(/^static.*/)) {
            newState[key] = this.state[key];
        }
    });
    return newState;
  }

  getAccountKey(accountNumber) {
    const account = this.state.staticAccounts[accountNumber];
    return account && account.key;
  }
}

export default Compound;
