#ifndef PEER_COMMAND_RUNNER_H_
#define PEER_COMMAND_RUNNER_H_

#ifdef _WIN32
#define _WIN32_WINNT 0x0600 // This is for asio
#endif // _WIN32
#include <asio.hpp>
#include <future>
#include <unordered_set>
#include <queue>

#include "build.h"

using namespace std;
using asio::ip::tcp;

struct PeerCommandRunner;

/// Command
struct Command {
  Command(int id, string command, string path) :
    id(id), command(command), path(path) {};

  int id;
  string command;
  string path;
};

/// Response
struct Response {
  Response(int id, int status, string message) :
    id(id), status(status), message(message) {};

  int id;
  int status;
  string message;
};

///
struct Peer {
  bool Connect();
  bool RunCommand(Command command);
  void ReadResponse();

  string m_address;
  asio::io_service* m_io_service;
  string host;
  tcp::socket* m_socket;
  asio::streambuf response_buff;
  bool available = false;

  PeerCommandRunner *command_runner;
};

/// Command Runner distribute command to peers
struct PeerCommandRunner : public CommandRunner {
  explicit PeerCommandRunner(const BuildConfig& config);
  virtual ~PeerCommandRunner() {}
  virtual bool CanRunMore();
  virtual bool StartCommand(Edge* edge);
  virtual bool WaitForCommand(Result* result);
  virtual vector<Edge*> GetActiveEdges();
  virtual void Abort();

  vector<string> GetPeerAddresses();
  void ReceiveResponse(Response resp);

  static std::atomic_int g_IdCounter;
  static PeerCommandRunner *g_PeerCommandRunner;

  const BuildConfig& config_;
  vector<Peer*> peers_;
  map<int, Edge*> id_to_edge_;
  queue<Response> responses_;
};


#endif
