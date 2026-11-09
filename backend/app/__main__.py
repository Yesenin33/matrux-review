import os

import uvicorn

LOGGING = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {"line": {"format": "%(asctime)s %(levelname)s %(name)s %(message)s"}},
    "handlers": {"stderr": {"class": "logging.StreamHandler", "formatter": "line"}},
    "loggers": {
        "palma": {"handlers": ["stderr"], "level": "INFO", "propagate": False},
        "uvicorn.error": {"handlers": ["stderr"], "level": "INFO", "propagate": False},
    },
}

if __name__ == "__main__":
    uvicorn.run(
        "app.main:create_app",
        factory=True,
        host=os.environ.get("PALMA_HOST", "127.0.0.1"),
        port=int(os.environ.get("PALMA_PORT", "8000")),
        access_log=False,
        log_config=LOGGING,
        server_header=False,
    )
