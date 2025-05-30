from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import jwt, JWTError
from starlette.status import HTTP_401_UNAUTHORIZED
from pathlib import Path

ALGORITHM = "RS256"
PUBLIC_KEY_PATH = Path(__file__).parent.parent.parent / "public.pem"

with open(PUBLIC_KEY_PATH, "r") as f:
    PUBLIC_KEY = f.read()

security = HTTPBearer()  # Extracts Bearer token from Authorization header

def verify_jwt_token(
        credentials: HTTPAuthorizationCredentials = Depends(security)
):
    token = credentials.credentials
    try:
        payload = jwt.decode(token, PUBLIC_KEY, algorithms=[ALGORITHM])
        # optionally: verify claims like exp, iss, aud here
        return payload  # Return decoded claims for downstream usage if needed
    except JWTError as e:
        raise HTTPException(
            status_code=HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired JWT token",
        )
